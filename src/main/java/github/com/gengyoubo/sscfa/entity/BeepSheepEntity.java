package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 月尘魔法·咩弹（召唤系「羊了个??Beep Sheep」，jackcooper??026-09-29 用户定稿）?
 *
 * <p><b>飞行轨迹与鸡蛋一??/b>：继?{@link ThrowableItemProjectile} 保留默认重力??.03 阻??+ 0.05 重力），
 * 呈标准抛物线。命中活体后将??b>变成?/b>（施?{@link SscAddon#SHEEP_FORM} 效果?
 * 客户端由 SheepFormClientHooks 用原版羊模型替换渲染），期间目标无法攻击 / 无法破坏 / 无法放置方块?/p>
 *
 * <p><b>目标限制??026-09-29 用户定稿改血量门?/b>：Boss 类（凋灵/监守者）??b>最大生命??
 * 超??50 ??/b>的目标免疫（原「碰撞箱??1.2 或??2.0」体型门已移除——mod NPC 普遍身高
 * ??2.0 被误杀，导致「NPC 生物无法变成羊」；血量门天然覆盖村民 20 血/多??NPC，同时仍
 * 挡住铁傀?100 血/末影?200 血/凋??300 血等大块头）?/p>
 *
 * <p>默认白名单：主人在线且目标受保护（玩家及其宠??召唤物）则不施加。命?Boss 免疫体时
 * 弹体反被「羊化反弹」演出（咩??+ 粒子），不施加效果??/p>
 */
public class BeepSheepEntity extends ThrowableItemProjectile {

	/** 血量门阈值（2026-09-29 用户定稿）：最大生命??> 50 点的目标免疫变羊??*/
	private static final float MAX_TARGET_HEALTH = 50.0F;

	/** 变羊时长（tick），DataTracker 无需——仅服务端结算用，NBT 持久化?*/
	private int sheepDuration = 120;
	/** 魔法等级??-5）??*/
	private int spellLevel = 1;
	/** 书内施法编号（solo ??null；流派命中返还用）??*/
	private java.util.UUID refundCastId;

	public void setRefundCastId(java.util.UUID castId) {
		refundCastId = castId;
	}

	public BeepSheepEntity(EntityType<? extends BeepSheepEntity> entityType, Level world) {
		super(entityType, world);
	}

	public BeepSheepEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.BEEP_SHEEP.get(), owner, world);
	}

	@Override
	protected Item getDefaultItem() {
		// 渲染物品：白色染料最接近「羊」意象（小白色方块弹道，FlyingItemEntityRenderer 直接用）
		return Items.WHITE_DYE;
	}

	@Override
	public void tick() {
		super.tick();
        if(this.isRemoved())return;
		// 飞行拖尾（服务端撒，天然多人同步）：白色羊毛?+ 轻微咩概率音不刷（弹道安静，命中才叫?
		if (this.level() instanceof ServerLevel serverWorld && this.tickCount % 2 == 0) {
			serverWorld.sendParticles(ParticleTypes.WHITE_ASH,
					this.getX(), this.getY(), this.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
		// 超时自毁?? 秒；鸡蛋式抛物线落地会触??onCollision，此为兜底??
		if (this.tickCount > 160) {
			this.discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		Entity target = entityHitResult.getEntity();
		if (this.level().isClientSide || !(target instanceof LivingEntity livingTarget)) {
			return;
		}
		// 默认白名单：主人在线且目标受保护 ??不施加（弹体消散?
		if (this.getOwner() instanceof ServerPlayer ownerPlayer
				&& WhitelistUtils.isProtected(ownerPlayer, livingTarget)) {
			return;
		}
		// Boss / 大型生物免疫（用户定稿：不允许变 boss 类生物，只能变小型生物）
		if (isImmuneTarget(livingTarget)) {
			// 免疫演出：咩一?+ 粒子弹开（幽默反馈：羊魔法对大块头无效??
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.SHEEP_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.6f);
			if (this.level() instanceof ServerLevel serverWorld) {
				serverWorld.sendParticles(ParticleTypes.SNEEZE,
						target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
			}
			return;
		}
		// 施加变羊：amplifier = 等??1（L1=0 ??L5=4，??lang/扩展显示??
		boolean hadHarmfulEffect = github.com.gengyoubo.sscfa.spell.FormCastingStyle.hasHarmfulEffect(livingTarget);
		boolean applied = livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get(), sheepDuration, spellLevel - 1, false, true, true), this.getOwner());
        if (github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(
                livingTarget,github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get(),sheepDuration,spellLevel-1,applied))
            github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(this,livingTarget);
		if (applied) {
			// 登记「受惊逃离源」（2026-09-29 用户定稿：变??NPC 的??AI 默认受惊远离施法者??
			github.com.gengyoubo.sscfa.effect.SheepFormAiController.setFleeFrom(
					livingTarget.getUUID(), this.getOwner() != null ? this.getOwner().getUUID() : null);
		}
		if (applied && this.getOwner() instanceof ServerPlayer caster) {
			github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,livingTarget);
			github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(caster, livingTarget,
					github.com.gengyoubo.sscfa.spell.FormationElement.SUMMON, refundCastId, hadHarmfulEffect);
		}
		// 变身演出：咩? 羊毛爆散
		this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.SHEEP_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.0f);
		if (this.level() instanceof ServerLevel serverWorld) {
			serverWorld.sendParticles(ParticleTypes.POOF,
					target.getX(), target.getY(0.6), target.getZ(), 12, 0.35, 0.5, 0.35, 0.03);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			// 落地演出：小团羊毛尘（不咩叫——只命中活体才叫??
			if (this.level() instanceof ServerLevel serverWorld) {
				serverWorld.sendParticles(ParticleTypes.POOF,
						this.getX(), this.getY(), this.getZ(), 5, 0.15, 0.15, 0.15, 0.02);
			}
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 排除盔甲架与施法者本人（同诅咒标记弹?
		return super.canHitEntity(entity) && entity != this.getOwner()
				&& entity instanceof LivingEntity && !(entity instanceof ArmorStand);
	}

	/** Boss / 高血量目标免疫判定（服务端权威，2026-09-29 用户定稿血量门）：凋灵、监守者??
	 *  最大生命??> 50 点。体型门已移除（mod NPC 身高普??>2.0 被误挡，村??低??NPC 现在可变羊）?*/
	public static boolean isImmuneTarget(LivingEntity target) {
		if (target instanceof WitherBoss
				|| target instanceof net.minecraft.world.entity.monster.warden.Warden) {
			return true;
		}
		return target.getMaxHealth() > MAX_TARGET_HEALTH;
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		refundCastId = nbt.hasUUID("RefundCastId") ? nbt.getUUID("RefundCastId") : null;
		if (nbt.contains("SheepDuration")) sheepDuration = nbt.getInt("SheepDuration");
		if (nbt.contains("SpellLevel")) spellLevel = nbt.getInt("SpellLevel");
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		if (refundCastId != null) nbt.putUUID("RefundCastId", refundCastId);
		nbt.putInt("SheepDuration", sheepDuration);
		nbt.putInt("SpellLevel", spellLevel);
	}

	/** 设置变羊时长（tick，服务端施法时调用）?*/
	public void setSheepDuration(int durationTicks) {
		this.sheepDuration = Math.max(20, durationTicks);
	}

	/** 设置魔法等级??-5）??*/
	public void setSpellLevel(int level) {
		this.spellLevel = Math.max(1, Math.min(5, level));
	}
}
