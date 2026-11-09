package github.com.gengyoubo.sscfa.spell.spells;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;

/**
 * 空间跳跃（空间系，绿色基底，jackcooper）：按住施法键在准星方向显示落点预览圈
 * （仿契灵平台传送交互 + 陨火按住瞄准管线），松开瞬移。
 *
 * <p><b>落点规则（三级回退）</b>：①沿视线逐格找最远合法落点（有地面 + 碰撞箱空间）；
 * ②无 → 传送到视线命中的第一个表面（方块命中面上表面）；③仍无 → 传到射程尽头
 * （尽头受阻则向起点内收至最后无阻挡处）。</p>
 *
 * <p>数值外置 {@code data/ssc_addon/spells/space_blink.json}：
 * 基准距离 8 格（每级 +25%）/ cd 16s（L5 8s，每级 -2s）/ 耗蓝 30（每级 +25%）；
 * 吃空间法阵距离加成。</p>
 */
public class SpaceBlinkSpell extends Spell {

	/** 基础瞬移距离（格）默认，实际 = 基础 × speed_multiplier(level) × 空间法阵距离加成；运行时从 balance 快照读取。 */
	private static final double BASE_RANGE = 8.0;

	// 阶段 5：运行时快照读取（spells.space_blink；快照未初始化回退默认常量）。
	// 瞄准距离被客户端按住预览（getAimMaxRange 落点圈）与服务端结算共用 → 双端一致：
	// 客户端读客户端镜像快照，服务端读权威快照（同 TidalOrbEntity.tetherSoftRadius 模式）。
	private static final BalanceReader BAL = new BalanceReader("spells.space_blink");

	/** 基础瞬移距离（双端一致）：客户端读客户端镜像，服务端读权威快照。 */
	private static double baseRange() {
		if (BalanceIntegration.isClientThread()) {
			var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.clientSnapshot();
			if (s != null) return s.getDouble("spells.space_blink", "base_range");
		}
		return BAL.d("base_range", BASE_RANGE);
	}

	public SpaceBlinkSpell() {
		super(new ResourceLocation("ssc_addon", "space_blink"), SpellRarity.GREEN);
	}

	/** 当前有效瞬移距离（客户端预览/服务端结算共用）。 */
	public double getBlinkRange(int level) {
        return RuneCastContext.current().scale(baseRange() * getSpeedMultiplier(level),RuneModifiers.Stat.DISTANCE)
                * RuneCastContext.formationRange();
	}

	/** 按住瞄准型：最大施法距离（客户端按住显示落点预览，松开施放）。 */
	@Override
	public double getAimMaxRange() {
		return getAimMaxRange(1);
	}

	@Override
	public double getAimMaxRange(int level) {
		return getBlinkRange(level);
	}

	/** 预览圈半径：小圈标记落点（0.5 格落地标记，同契灵平台传送视觉语言）。 */
	@Override
	public double getAimRadius(int level) {
		return 0.5;
	}

	/** 实际有效射程（含等级缩放）。 */
	private double effectiveRange(int level) {
		return getBlinkRange(level);
	}

	@Override
	public boolean canCast(ServerPlayer caster) {
		return !github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.teleportBlocked(caster);
	}

    @Override public net.minecraft.network.chat.Component getCastFailureMessage(ServerPlayer caster) {
        return net.minecraft.network.chat.Component.translatable("message.ssc_addon.anomaly.teleport_blocked");
    }
    @Override public boolean canContinueCasting(ServerPlayer caster, net.minecraft.world.item.ItemStack scroll) {
        return canCast(caster);
    }

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(caster)) {
            github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.miss(); return;
        }
		if (!(caster.level() instanceof ServerLevel serverWorld)) {
			return;
		}
		Vec3 dest = getCastTarget(caster, level);
		if (dest == null) {
			return; // 理论不可达（回退链已兜底），防御性保留
		}
		// 起点消散演出
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, caster, ParticleTypes.PORTAL,
				caster.getX(), caster.getY(0.5), caster.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
		caster.teleportToWithTicket(dest.x, dest.y, dest.z);
		// 终点汇聚演出
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, caster, ParticleTypes.END_ROD,
				dest.x, dest.y + 1.0, dest.z, 16, 0.3, 0.5, 0.3, 0.05);
		serverWorld.playSound(null, dest.x, dest.y, dest.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.4f);
	}

	/**
	 * 计算瞬移落点（三级回退）：
	 * ①沿视线逐格找最远合法落点（有地面 + 碰撞箱空间）；
	 * ②无 → 视线命中的第一个表面（方块命中面上表面）；
	 * ③仍无 → 射程尽头点（受阻则从尽头向起点内收到最后无阻挡处）。
	 */
	@Override
	public Vec3 captureCastTarget(ServerPlayer caster, int level) {
		Vec3 start = caster.position();
		Vec3 dir = caster.getViewVector(1.0F).normalize();
		double range = effectiveRange(level);
		float width = caster.getDimensions(net.minecraft.world.entity.Pose.STANDING).width;
		float height = caster.getDimensions(net.minecraft.world.entity.Pose.STANDING).height;

		// ① 首选：最远合法落点（有地面 + 空间无阻挡；领域跨界候选不算合法——防 blink 逃出领域）
		Vec3 best = null;
		for (double d = 1.5; d <= range; d += 0.5) {
			Vec3 p = start.add(dir.scale(d));
			BlockPos feet = BlockPos.containing(p.x, p.y, p.z);
			if (caster.level().noCollision(caster, boxAt(p, width, height))
					&& !caster.level().getBlockState(feet.below()).isAir()
					&& !github.com.gengyoubo.sscfa.spell.DomainManager
						.blocksAimBoundary(caster.level(), start, p)) {
				best = p;
			}
		}
		if (best != null) {
			return best;
		}

		// ② 回退：视线命中的第一个表面（脚部贴命中方块上表面）
		Vec3 eye = caster.getEyePosition();
		Vec3 end = eye.add(dir.scale(range));
		BlockHitResult hit = caster.level().clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		if (hit.getType() == HitResult.Type.BLOCK) {
			Vec3 surface = Vec3.atBottomCenterOf(hit.getBlockPos());
			if (caster.level().noCollision(caster, boxAt(surface, width, height))) {
				return surface;
			}
		}

		// ③ 兜底：射程尽头（受阻则向起点内收至最后无阻挡点）
		for (double d = range; d >= 1.0; d -= 0.5) {
			Vec3 p = start.add(dir.scale(d));
			if (caster.level().noCollision(caster, boxAt(p, width, height))) {
				return p;
			}
		}
		// ④ 终极兜底（2026-09-19）：极端挤压几何（贴墙/低天花板下 1 格内也无净空）时回退原地，
		// 不返回 null——null 会让松手释放被当作「无目标」走打断路径，玩家看到「施法被打断」误报。
		return start;
	}

	/** 构造落点判定盒（玩家碰撞箱，脚部对齐 p）。 */
	private static AABB boxAt(Vec3 p, float width, float height) {
		return new AABB(
				p.x - width / 2.0, p.y, p.z - width / 2.0,
				p.x + width / 2.0, p.y + height, p.z + width / 2.0);
	}
}
