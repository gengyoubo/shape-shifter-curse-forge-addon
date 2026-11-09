package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.ability.FrostSpikeManager;

import java.util.Optional;
import java.util.UUID;

/**
 * 寒棘狐「冰刺」冰锥实体：一个实体承担两种状态—?
 * <ul>
 *   <li><b>HOVER（环绕态）</b>：蓄力凝聚后环绕玩家斜上方漂浮，位置?{@code FrostSpikeManager} ?tick 设置?
 *       各自独立计时，最多存?{@value #MAX_HOVER_TICKS} tick?0 秒）后自行消失；材质随存在时间分 3 阶段?/li>
 *   <li><b>FLY（飞行态）</b>：发射后?{@value #SPEED} ?tick 直线飞行，超?{@value #STRAIGHT_DIST} 格后?tick 下坠?
 *       直击造成 {@value #DAMAGE} 点物理伤害（默认白名单豁免），撞方块 / 超时 5 秒自毁?/li>
 * </ul>
 * 无论何种状态，主人?SP 悦灵净化（{@link SscAddon#PURIFIED}）时立即碎裂?
 * 全部判定在服务端，实体走原版 EntityTracker 天然多人同步；飞行速度?DataTracker 全精度同步避免生成包截断失真?
 */
public class FrostThornEntity extends Projectile {

	public static final int STATE_HOVER = 0;
	public static final int STATE_FLY = 1;

	private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.INT);
	// 环绕 slot 索引 + 主人 UUID：供客户?HOVER 态按本地玩家自算贴合位置（平滑跟手，不依赖网络包跳位?
	private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Float> VEL_X = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> VEL_Y = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> VEL_Z = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.FLOAT);
	// 强化等级（凝棘次技能）?=普通冰刺；>=1=强化直飞冰锥（消?N 个环绕冰锥凝成），同步客户端用于渲染放大
	private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.INT);
        // 新生标记：仅服务端真正凝聚出的新锥为 true（成形粒子只跟它）；重进存档恢复的旧锥为 false，不重播成形特效
        private static final EntityDataAccessor<Boolean> FRESH = SynchedEntityData.defineId(FrostThornEntity.class, EntityDataSerializers.BOOLEAN);

	private static final double SPEED = 0.8;          // 16 鏍?绉?
	private static final double STRAIGHT_DIST = 16.0; // 16 格内直线，之后下?
	private static final double GRAVITY = 0.03;       // 16 格后?tick 竖直衰减
	private static final int MAX_FLY_TICKS = 100;     // 5 秒超时自?
	private static final int MAX_HOVER_TICKS = 1200;  // 60 秒存在时?
	private static final double MAX_FLY_DIST = 128.0; // 最远飞?128 ?
	private static final float DAMAGE = 8.0f;
	private static final double CONVERGE = 0.6;       // 向准星射线靠拢的弯曲强度（越大越急弯?
	private static final double CONVERGE_DONE = 0.35; // 靠拢完成阈值（格），汇入射线后转直?
	// ===== 凝棘强化直飞（次技能）=====
	private static final int ENHANCED_MAX_FLY_TICKS = 200;  // 10 秒后自动消失（无距离销毁、无下坠?
	private static final float ENHANCED_BASE_DAMAGE = 8.0f; // 每消耗一个冰?+100%：伤?= 8×(1+level)
	// 强化冰锥专用并线参数：并线路??初始偏差/弯曲强度（与速度无关）。大冰锥从头顶出发偏差约 0.5~1 格，
	// 用低弯曲强度把弧线拉长到?9 格（偏转角仅 ~3.4°，平缓滑入准星线，用距离换视觉效果，防近处猛拐）?
	// 阈值收紧到 0.1 格——主技?0.35 格残差对小冰锥看不出来，大冰锥会肉眼可见「没在射线上?
	private static final double ENHANCED_CONVERGE = 0.06;
	private static final double ENHANCED_CONVERGE_DONE = 0.1;
	private static final int FLIGHT_SOUND_INTERVAL = 7;  // 飞行中每 7 tick 播一次高速划破空气音（连续呼啸）

	// 阶段 5：服务端权威快照读取（快照未初始化回退默认常量）；物理插值（GRAVITY/CONVERGE 系列）与
	// 环绕摆位（HOVER_*/SLOT_*/MAX_THORN_TURN）不?schema，保持常量直?
	private static final BalanceReader BAL = new BalanceReader("abilities.frost_thorn");

	// ===== 环绕几何（锺点参照雪?FeralBody 胸部方块：背部竖直面 150° 开屏，服务?/ 客户端共用一套计算） =====
	private static final double HOVER_RADIUS = 0.7;
	private static final double HOVER_HEIGHT = 0.45;
	private static final double BACK_DIST = 0.25;    // 扇形圆心沿背后方向的偏移
	/** 冰锥与身体朝向平行，尖朝头部所指方向，水平/垂直最多各沿这条平行线偏转此角度?*/
	private static final float MAX_THORN_TURN = 45f;
	/** 5 ?slot 的仰角（度）：上→左→右→左上→右上；扇形以正上为中心对称跨 150°（间?37.5°）?*/
	private static final double[] SLOT_ELEV = {90, 15, 15, 52.5, 52.5};
	/** 5 ?slot 的左右符号（沿玩家左手方向为正）：上=0、左=+1、右=-1、左?+1、右?-1?*/
	private static final int[] SLOT_SIDE = {0, 1, -1, 1, -1};

	private int hoverTicks = 0;
	private int flyTicks = 0;
	private Vec3 flyStart;
	private Vec3 rayOrigin;              // 发射瞬间的准星射线起点（玩家眼睛?
	private Vec3 rayDir;                 // 发射瞬间的准星射线方向（归一化）
	private boolean converging = false;   // 是否处于向准星射线靠拢阶?
	private double enhancedSpeed = BAL.d("speed", SPEED); // 凝棘强化冰锥飞行速度（converging 靠拢/直飞共用；真正发射时?launchEnhanced 重读快照?

	public FrostThornEntity(EntityType<? extends FrostThornEntity> type, Level world) {
		super(type, world);
	}

	public FrostThornEntity(Level world, Player owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.FROST_THORN.get(), world);
		this.setOwner(owner);
		this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
		this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(STATE, STATE_HOVER);
		this.entityData.define(STAGE, 0);
		this.entityData.define(SLOT, 0);
		this.entityData.define(OWNER_UUID, Optional.empty());
		this.entityData.define(VEL_X, 0.0f);
		this.entityData.define(VEL_Y, 0.0f);
		this.entityData.define(VEL_Z, 0.0f);
		this.entityData.define(LEVEL, 0);
		this.entityData.define(FRESH, false);
	}

	public int getState() { return this.entityData.get(STATE); }
	public int getStage() { return this.entityData.get(STAGE); }
	/** 强化等级?=普通冰刺，>=1=凝棘强化冰锥（渲染放?+ 命中伤害用）?*/
	public int getLevel() { return this.entityData.get(LEVEL); }
	public int getHoverTicks() { return hoverTicks; }
	/** One accepted ice spell spends lifespan once, including a final full discount from a nearly expired thorn. */
	public int spendHoverLifetime(int ticks) {
		if(level().isClientSide||isRemoved()||!isHover()||ticks<=0)return 0;
		int maximum=BAL.i("max_hover_ticks",MAX_HOVER_TICKS);
		int spent=Math.min(ticks,Math.max(0,maximum-hoverTicks));
		hoverTicks=(int)Math.min(maximum,(long)hoverTicks+ticks);
		if(hoverTicks>=maximum)shatter();else updateStage();
		return spent;
	}
	public boolean isHover() { return getState() == STATE_HOVER; }
	public void setSlot(int slot) { this.entityData.set(SLOT, slot); }
	public int getSlot() { return this.entityData.get(SLOT); }
	/** 标记为新生冰锥（服务端凝聚时调用）：成形粒子只在新生锥身上播，重进恢复的旧锥不播?*/
	public void markFresh() { this.entityData.set(FRESH, true); }
	/** 主人 UUID（DataTracker，重载后?NBT 恢复）——客户端跟随 / 服务端自认领都用它找回主人?*/
	public Optional<UUID> getOwnerUuid() { return this.entityData.get(OWNER_UUID); }
	/** 重进重建时恢复已存在 tick（阶段材?剩余寿命延续退出前状态）?*/
	public void restoreHoverTicks(int ticks) {
		this.hoverTicks = Math.max(0, ticks);
		updateStage();
	}

	/** 环绕目标位置（服务端 / 客户端共用同一套几何）：背部竖直面 150° 开屏?*/
	public static Vec3 hoverTarget(LivingEntity owner, int slot) {
		float bodyYaw = owner.yBodyRot;
		double bodyRad = Math.toRadians(bodyYaw);
		// MC 水平朝向 facing=(-sin,0,cos)，背?(sin,0,-cos)；左侧方?left=(cos,0,sin)
		double centerX = owner.getX() + Math.sin(bodyRad) * BACK_DIST;
		double centerZ = owner.getZ() - Math.cos(bodyRad) * BACK_DIST;
		double centerY = owner.getY() + HOVER_HEIGHT;
		double leftX = Math.cos(bodyRad);
		double leftZ = Math.sin(bodyRad);
		double th = Math.toRadians(SLOT_ELEV[slot]);
		double a = SLOT_SIDE[slot] * Math.cos(th);
		double b = Math.sin(th);
		return new Vec3(centerX + leftX * a * HOVER_RADIUS, centerY + b * HOVER_RADIUS, centerZ + leftZ * a * HOVER_RADIUS);
	}

	/** 环绕朝向（水平）：与身体朝向平行，尖朝头部所指方向，最多偏 ±MAX_THORN_TURN?*/
	public static float hoverYaw(LivingEntity owner) {
		float headDelta = Mth.wrapDegrees(owner.yHeadRot - owner.yBodyRot);
		return owner.yBodyRot + Mth.clamp(headDelta, -MAX_THORN_TURN, MAX_THORN_TURN);
	}

	/** 环绕朝向（垂直）：尖随头部俯仰抬?压低，最多偏 ±MAX_THORN_TURN?*/
	public static float hoverPitch(LivingEntity owner) {
		return Mth.clamp(owner.getXRot(), -MAX_THORN_TURN, MAX_THORN_TURN);
	}

	/** 客户?HOVER 态：按本地玩家每 tick 自算贴合位置（渲?lerp(prev, cur)，prev 先同步防瞬移包拖影）?*/
	private void hoverFollowClient() {
		Optional<UUID> oid = this.entityData.get(OWNER_UUID);
		if (oid.isEmpty()) return;
		Player p = this.level().getPlayerByUUID(oid.get());
		if (p == null) return;
		// 先同?prev 位置/朝向再写入新值（渲染?lerp(prev, cur, tickDelta)）：
		// 防服务端 40 格校正的瞬移包造成「从旧位置到新位置」的一帧插值拖影闪现，也保证朝向变化平滑插?
		this.xo = this.getX();
		this.yo = this.getY();
		this.zo = this.getZ();
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
		Vec3 target = hoverTarget(p, getSlot());
		this.setPos(target.x, target.y, target.z);
		// 钉死渲染插值源：trackedPos ?EntityRenderer 平移/lerp 的唯一数据源，
		// 若不重置，服务器同步包（生成?校正位等旧值）会周期性喂入缓冲、逐帧向旧位逼近?
		// 与本地自算位置打架——表现为冰锥周期性「自后向前蹿一下」。用公开 API 更新插值源?
		this.syncPacketPositionCodec(target.x, target.y, target.z);
		this.setYRot(hoverYaw(p));
		this.setXRot(hoverPitch(p));		// 成形汇聚（前 20t）：在自身当前位置发向内汇聚粒子（客户端本地）?
		// 仅新生冰锥（服务端真正凝聚出的，FRESH 同步）才播——重进存档恢复的旧锥 age 同样?0 起算?
		// 不加此标记的话每次重进游戏所有旧锥都会重播一遍成形特效（头顶 slot 0 最显眼，即用户看到的误播）?
		if (this.tickCount < 20 && this.entityData.get(FRESH)) {
			for (int i = 0; i < 2; i++) {
				double u = this.random.nextDouble() * 2 - 1;
				double theta = this.random.nextDouble() * Math.PI * 2;
				double r = Math.sqrt(1 - u * u);
				double dx = r * Math.cos(theta), dy = u, dz = r * Math.sin(theta);
				double speed = 1.0 / 20.0; // 与服务端同款??20t，抵达中心即寿命尽消?
				this.level().addParticle(github.com.gengyoubo.sscfa.init.AddonParticles.INWARD_ICE.get(),
						this.getX() + dx, this.getY() + dy, this.getZ() + dz,
						-dx * speed, -dy * speed, -dz * speed);
			}
		}		// 环境粒子：仿雪狐 ambient 粒子机制（apoli particle power：零初速生成，靠粒子自身物理缓飘）
		// 白色小冰晶密度为上一版的 10%?00 tick 一朵）；雪花概率为上一版的 30%?/100?
		// 每个客户端各自本地生成（所有客户端都自算贴合位置，各视角一致）
		if (this.tickCount % 100 == 0) {
			this.level().addParticle(ParticleTypes.WHITE_ASH, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
		}
		if (this.random.nextInt(100) == 0) {
			this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
		}
	}

	/** FrostSpikeManager ?tick 设置 HOVER 冰锥的位置与朝向（环绕玩家斜上方）?*/
	public void setHoverTransform(Vec3 pos, float yaw, float pitch) {
		this.setPos(pos.x, pos.y, pos.z);
		this.setYRot(yaw); this.yRotO = yaw;
		this.setXRot(pitch); this.xRotO = pitch;
	}

	/** 发射：切 FLY 态，从当?环绕)位置出发，飞行中靠拢到发射瞬间的准星射线?*/
	public void launch(Vec3 rayOrigin, Vec3 rayDir) {
		this.entityData.set(STATE, STATE_FLY);
		this.flyStart = this.position();
		this.flyTicks = 0;
		this.rayOrigin = rayOrigin;
		this.rayDir = rayDir.normalize();
		this.converging = true;
		Vec3 v = this.rayDir.scale(BAL.d("speed", SPEED));
		this.setDeltaMovement(v.x, v.y, v.z);
		this.entityData.set(VEL_X, (float) v.x);
		this.entityData.set(VEL_Y, (float) v.y);
		this.entityData.set(VEL_Z, (float) v.z);
		updateRotationFromVelocity(v);
	}

	/** 凝棘发射：强化冰锥（消?level 个环绕冰锥凝成）。从头顶法阵中心沿调用方给定的方?*纯直?*飞行（尖朝速度方向，无弯曲汇入——弯曲拐弯会被看成「旋转着飞」），无下坠、无距离销毁，{@value #ENHANCED_MAX_FLY_TICKS} tick(10s) 后消失?*/
	public void launchEnhanced(Vec3 origin, Vec3 dir, int level, Vec3 rayOrigin, Vec3 rayDir) {
		this.entityData.set(LEVEL, Math.max(1, level));
		this.entityData.set(STATE, STATE_FLY);
		this.setPos(origin.x, origin.y, origin.z);
		this.flyStart = origin;
		this.flyTicks = 0;
		this.rayOrigin = rayOrigin;
		this.rayDir = rayDir.normalize();
		this.converging = true;   // 发射即垂足靠拢并入准星中线（横向位置偏差收敛?CONVERGE_DONE 后沿准星线直飞）
		this.enhancedSpeed = BAL.d("speed", SPEED) * (1.0 + 0.5 * level); // 每消耗一个冰?+50% 飞行速度（基础 16 ?s?
		Vec3 v = dir.normalize().scale(this.enhancedSpeed);
		this.setDeltaMovement(v.x, v.y, v.z);
		this.entityData.set(VEL_X, (float) v.x);
		this.entityData.set(VEL_Y, (float) v.y);
		this.entityData.set(VEL_Z, (float) v.z);
		updateRotationFromVelocity(v);
	}

	private void updateRotationFromVelocity(Vec3 v) {
		// 渲染器绕 Y(+yaw)/X(+pitch) 旋转且模型尖?+Z：yaw/pitch 取负号才能让尖端指向速度方向
		double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
		this.setYRot((float) (Mth.atan2(-v.x, v.z) * (180.0 / Math.PI)));
		this.setXRot((float) (Mth.atan2(-v.y, horiz) * (180.0 / Math.PI)));
	}

	/**
	 * 客户?FLY 态：忽略服务?move/rotate 跟踪包（位置 + 朝向全部以本地外推为唯一权威）?
	 *
	 * <p>反编?{@code EntityTrackerEntry} 实证：服务端?tick 发的 rotate 包携带的是上一 tick 的朝?
	 * （网络延?1~3t + 256 级量化），基?{@code updateTrackedPositionAndAngles} 对非生物实体?
	 * 无插值的直接 setRotation——网络旧值与本地按速度自算的新朝向?tick 相互拉锯?
	 * 弯曲段（向准星射线靠拢，yaw ?tick 大变）被放大成「旋转着飞」?
	 * 位置/速度?DataTracker 全精度每 tick 同步（VEL_X/Y/Z），本地外推已自洽，忽略跟踪包即可根治?/p>
	 */
	@Override
	public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
		if (this.level().isClientSide && getState() == STATE_FLY) return; // FLY 态：外推权威，忽略网络包
		super.lerpTo(x, y, z, yaw, pitch, interpolationSteps, interpolate);
	}

	@Override
	public void lerpHeadTo(float yaw, int interpolationSteps) {
		if (this.level().isClientSide && getState() == STATE_FLY) return;
		super.lerpHeadTo(yaw, interpolationSteps);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) {
			// 客户端外?FLY 位置（用 DataTracker 全精度速度覆盖被生成包截断?velocity?
			if (getState() == STATE_FLY) {
				Vec3 v = new Vec3(entityData.get(VEL_X), entityData.get(VEL_Y), entityData.get(VEL_Z));
				// 速度数据包比生成包晚?1~2 tick：未到前 VEL 为初?0，若照常更新会把朝向 atan2(0,0)=0 归零?
				// 数据到达后跳回正确朝??起飞瞬间「先甩头再归位」。未到前不动不转，保持生成包携带的正确姿态?
				if (v.lengthSqr() > 1.0e-8) {
					this.setDeltaMovement(v.x, v.y, v.z);
					this.yRotO = getYRot(); this.xRotO = getXRot();
					this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
					// 朝向与速度同步：客户端按与服务端同款公式自算，平滑贴合轨迹（单/多人一致）
					updateRotationFromVelocity(v);
				}
			} else {
				hoverFollowClient();  // HOVER 态：按本地玩家自算贴合，平滑跟手
			}
			return;
		}
		// 重载的飞行残留：速度/起点不持久化，重进后会冻在空中——直接清?
		if (getState() == STATE_FLY && this.flyStart == null) {
			this.discard();
			return;
		}
		// 净化消除：主人?SP 悦灵净??无论环绕 / 飞行都碎裂（owner 引用在下?adopt 中恢复）
		if (this.getOwner() instanceof ServerPlayer op && op.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			shatter();
			return;
		}
		if (getState() == STATE_HOVER) {
			hoverTicks++;
			updateStage();
			if (hoverTicks > BAL.i("max_hover_ticks", MAX_HOVER_TICKS)) { shatter(); return; } // 存在时间到期
			// 重进游戏自找回：ProjectileEntity ?owner 不写 NBT、管理器静态状态也不持久化?
			// 重载后环绕冰锥成孤儿——用 OWNER_UUID 找回主人并重新挂进管理器（幂等，已认领则跳过?
			FrostSpikeManager.adopt(this);
		} else {
			tickFly();
		}
	}

	private void updateStage() {
		int stage = hoverTicks < 400 ? 0 : (hoverTicks < 800 ? 1 : 2);
		if (this.entityData.get(STAGE) != stage) this.entityData.set(STAGE, stage);
	}

	private void tickFly() {
		flyTicks++;
		// 阶段 5：同函数多次读取局部变量化
		double speed = BAL.d("speed", SPEED);
		int enhancedMaxFlyTicks = BAL.i("enhanced_max_fly_ticks", ENHANCED_MAX_FLY_TICKS);
		// 凝棘强化冰锥：从法阵中央弯曲汇入准星射线（同主技能），汇入后直飞（无下坠、无距离销毁）?0 秒后消失；飞行中持续播高速划破空气音
		if (getLevel() > 0) {
			HitResult ehit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
			if (ehit.getType() != HitResult.Type.MISS) {
				this.onHit(ehit);
				if (this.isRemoved()) return;
			}
			Vec3 ev = this.getDeltaMovement();
			if (converging && rayDir != null) {
				// 真正并入准星中线（垂足靠拢）：朝射线上最近点偏转，横向偏差收敛到阈值后沿准星线直飞?
				// 强化冰锥用低弯曲强度（ENHANCED_CONVERGE）：弧线拉长?9 格平缓滑入，防近处猛拐；
				// 阈值收?0.1 格，大冰锥最终弹道与准星线重合（残差肉眼不可见）
				Vec3 pos = this.position();
				double t = Math.max(0.0, pos.subtract(rayOrigin).dot(rayDir));
				Vec3 proj = rayOrigin.add(rayDir.scale(t));
				Vec3 lateral = proj.subtract(pos);
				if (lateral.length() < ENHANCED_CONVERGE_DONE) {
					converging = false;
					ev = rayDir.scale(enhancedSpeed);
				} else {
					Vec3 cdir = rayDir.add(lateral.normalize().scale(ENHANCED_CONVERGE)).normalize();
					ev = cdir.scale(enhancedSpeed);
				}
				this.setDeltaMovement(ev);
				this.entityData.set(VEL_X, (float) ev.x);
				this.entityData.set(VEL_Y, (float) ev.y);
				this.entityData.set(VEL_Z, (float) ev.z);
			}
			this.yRotO = getYRot(); this.xRotO = getXRot();
			this.setPos(getX() + ev.x, getY() + ev.y, getZ() + ev.z);
			updateRotationFromVelocity(ev);
			if (this.level() instanceof ServerLevel sw) {
				sw.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 3, 0.08, 0.08, 0.08, 0.0);
				if (flyTicks % FLIGHT_SOUND_INTERVAL == 1) {
					// 高速划破空气音：用 FromEntity 包让声源跟随冰锥移动（非钉在播放坐标?
					Holder<SoundEvent> entry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.TRIDENT_RIPTIDE_3);
					ClientboundSoundEntityPacket pkt = new ClientboundSoundEntityPacket(entry, SoundSource.PLAYERS, this, 0.8f, 1.0f, this.random.nextLong());
					for (ServerPlayer p : sw.players()) {
						if (p.distanceToSqr(this) < 64.0 * 64.0) p.connection.send(pkt);
					}
				}
			}
			if (flyTicks > enhancedMaxFlyTicks) { this.discard(); }
			return;
		}
		// 碰撞判定仅服务端、移动前扫掠整段路径（高速不漏怪）
		HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hit.getType() != HitResult.Type.MISS) {
			this.onHit(hit);
			if (this.isRemoved()) return;
		}
		// 普通直飞分支参数（同函数多次读取局部变量化?
		double straightDist = BAL.d("straight_dist", STRAIGHT_DIST);
		double maxFlyDist = BAL.d("max_fly_dist", MAX_FLY_DIST);
		int maxFlyTicks = BAL.i("max_fly_ticks", MAX_FLY_TICKS);
		Vec3 v = this.getDeltaMovement();
		if (converging && rayDir != null) {
			// 向发射瞬间的准星射线自然靠拢：速度方向 = 射线方向 + 指向射线垂足的横向分量（随靠近而减小）
			Vec3 pos = this.position();
			double t = Math.max(0.0, pos.subtract(rayOrigin).dot(rayDir));
			Vec3 proj = rayOrigin.add(rayDir.scale(t));
			Vec3 lateral = proj.subtract(pos);
			if (lateral.length() < CONVERGE_DONE) {
				// 已汇入射??转直飞，从此点重新起?16 格直?
				converging = false;
				v = rayDir.scale(speed);
				this.flyStart = pos;
			} else {
				Vec3 dir = rayDir.add(lateral.normalize().scale(CONVERGE)).normalize();
				v = dir.scale(speed);
			}
			this.setDeltaMovement(v);
			this.entityData.set(VEL_X, (float) v.x);
			this.entityData.set(VEL_Y, (float) v.y);
			this.entityData.set(VEL_Z, (float) v.z);
		} else {
			// 直飞?6 格后?tick 下坠
			if (flyStart != null && this.distanceToSqr(flyStart) > straightDist * straightDist) {
				v = new Vec3(v.x, v.y - GRAVITY, v.z);
				this.setDeltaMovement(v);
				this.entityData.set(VEL_Y, (float) v.y);
			}
		}
		this.yRotO = getYRot(); this.xRotO = getXRot();
		this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
		updateRotationFromVelocity(v);
		if (this.level() instanceof ServerLevel sw) {
			sw.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.0);
		}
		if (flyStart != null && this.distanceToSqr(flyStart) > maxFlyDist * maxFlyDist) { this.discard(); return; }
		if (flyTicks > maxFlyTicks) { this.discard(); }
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (this.level().isClientSide) return;
		if (getState() != STATE_FLY) return;
		if (hitResult.getEntity() instanceof LivingEntity living) {
			// 默认白名单：豁免玩家 / 宠物 / 白名单个?
			boolean protectedTarget = this.getOwner() instanceof ServerPlayer op
					&& WhitelistUtils.isProtected(op, living);
			if (!protectedTarget) {
				float dmg = getLevel() > 0
						? (float) BAL.d("enhanced_base_damage", ENHANCED_BASE_DAMAGE) * (1 + getLevel())
						: (float) BAL.d("damage", DAMAGE);
				// 寒棘项圈：普通冰锥（主技能）伤害 ×50% + 真正命中敌人时立刻免费回?1 根环绕冰锥；
				// 强化冰锥（次技能）不受项圈影响
				if (this.getOwner() instanceof ServerPlayer op
						&& github.com.gengyoubo.sscfa.item.FrostSpineCollarItem.isWearingBy(op)
						&& getLevel() == 0) {
					dmg *= github.com.gengyoubo.sscfa.item.FrostSpineCollarItem.DAMAGE_MULTIPLIER;
					living.hurt(this.damageSources().mobAttack(this.getOwner() instanceof LivingEntity l ? l : null), dmg);
					github.com.gengyoubo.sscfa.ability.FrostSpikeManager.refundThorn(op);
				} else {
					living.hurt(this.damageSources().mobAttack(this.getOwner() instanceof LivingEntity l ? l : null), dmg);
				}
			}
		}
		shatter();
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level().isClientSide) return;
		if (getState() != STATE_FLY) return;
		if (hitResult.getType() == HitResult.Type.BLOCK) shatter();
	}

	/** 碎裂：冰晶粒?+ 玻璃碎裂音效 + 移除?*/
	private void shatter() {
		if (this.level() instanceof ServerLevel sw) {
			// 碎裂点用「当前真实环绕位」：HOVER 态服务端不?tick 移动（残留位置停在生成点），
			// 直接?getX() 会让碎裂粒子/结冰出现在冰锥最初诞生处——按主人+槽位现算真实位置
			Vec3 at = this.position();
			if (isHover() && this.getOwner() instanceof LivingEntity owner) {
				at = hoverTarget(owner, getSlot());
				this.setPos(at.x, at.y, at.z); // 同步刷新，结?音效也用真实?
			}
			sw.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.05);
			sw.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7f, 1.3f);
			// 寒冰入体：碎裂点周园结冰——水→冰、岩浆源→黑曜石、流动岩浆→圆石（同原版?岩浆碰撞性质?
			freezeAround(sw);
		}
		this.discard();
	}

	/**
	 * 碎裂点周围结冰（仅服务端）：普通冰锥半?1.5 格；凝棘强化冰锥随等级增大（1 ?2 ??5 ?4 格）?
	 * 照原版冰霜行者做法球形扫描替换源方块；岩浆按原版水碰撞规则分?流动两种产物?
	 */
	private void freezeAround(ServerLevel sw) {
		int level = getLevel();
		double radius = 1.5 + 0.5 * level;
		int r = (int) Math.ceil(radius);
		BlockPos base = blockPosition();
		boolean any = false;
		for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -r, -r), base.offset(r, r, r))) {
			double dx = pos.getX() + 0.5 - getX(), dy = pos.getY() + 0.5 - getY(), dz = pos.getZ() + 0.5 - getZ();
			if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
			var state = sw.getBlockState(pos);
			if (state.is(Blocks.WATER)) {
				sw.setBlock(pos, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
				any = true;
			} else if (state.is(Blocks.LAVA)) {
				// 源方?level=0 ?黑曜石；流动岩浆 ?圆石（原版水碰撞规则?
				boolean source = state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) == 0;
				sw.setBlock(pos, (source ? Blocks.OBSIDIAN : Blocks.COBBLESTONE).defaultBlockState(), Block.UPDATE_ALL);
				any = true;
			}
		}
		if (any) {
			sw.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5f, 0.6f);
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != this.getOwner() && entity instanceof LivingEntity;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putInt("State", getState());
		nbt.putInt("HoverTicks", hoverTicks);
		nbt.putInt("Slot", getSlot());
		// 主人 UUID 持久化：重进游戏后客户端跟随 / 服务端自认领都靠它找回主?
		getOwnerUuid().ifPresent(u -> nbt.putUUID("OwnerUuid", u));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		this.entityData.set(STATE, nbt.getInt("State"));
		this.hoverTicks = nbt.getInt("HoverTicks");
		updateStage();
		this.entityData.set(SLOT, nbt.getInt("Slot"));
		if (nbt.hasUUID("OwnerUuid")) {
			this.entityData.set(OWNER_UUID, Optional.of(nbt.getUUID("OwnerUuid")));
		}
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		// 碰撞盒仅 0.3³，而渲染模型（缩放 0.48 + 偏心补偿平移）延伸到锚点外约半格—?
		// 扩大可见盒避免视锥剔除把模型整帧剔除导致的周期性闪烁（?LaserBeamEntity 先例?
		return this.getBoundingBox().inflate(2.0);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
