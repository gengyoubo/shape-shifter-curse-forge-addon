package github.com.gengyoubo.sscfa.effect;

import github.com.gengyoubo.sscfa.util.TrinketUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import github.com.gengyoubo.sscfa.balance.BalanceReader;

public class PlayingDeadEffect extends MobEffect {
	// 装死回血：??10 tick 结算一次，6 ??120t)??12 ??
	// 默认：总回 15 颗??30HP) ??每??2.5 HP
	// 戴活珊瑚项链：总回 5 颗??10HP) + 20 颗黄?40吸收HP) ??每??0.834HP + 黄心累??3.34 封??40
	private static final float DEFAULT_HEAL_PER_TICK = 30.0f / 12.0f;
	private static final float NECKLACE_HEAL_PER_TICK = 10.0f / 12.0f;
	private static final float NECKLACE_ABSORB_PER_TICK = 40.0f / 12.0f;
	private static final float NECKLACE_ABSORB_MAX = 40.0f;

	// 阶??5：运行时快照读取（abilities.playing_dead；快照未初始化回退默认常量）?
	// 默认值???float 常量逐位一致??.5 / 0.8333333 / 3.3333333 / 40.0）??
	private static final BalanceReader BAL = new BalanceReader("abilities.playing_dead");

	public PlayingDeadEffect() {
		super(MobEffectCategory.BENEFICIAL, 0x586e7c);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.removeAttributeModifiers(entity, attributes, amplifier);
		entity.setSwimming(false);
		if (entity.getSleepingPos().isEmpty()
				&& (entity.getPose() == Pose.SWIMMING || entity.getPose() == Pose.SLEEPING)) {
			entity.setPose(Pose.STANDING);
		}
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity == null || entity.isDeadOrDying()) {
			return;
		}
		// Use SWIMMING pose which forces the model to lie flat (crawl animation) on client side
		entity.setPose(Pose.SWIMMING);
		entity.setSwimming(true);
		entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
		entity.hurtMarked = true;

		// 回血结算（仅服务端，???10 tick??
		if (entity.level().isClientSide() || entity.tickCount % 10 != 0) {
			return;
		}
		boolean hasNecklace = false;
		if (entity instanceof Player) {
			hasNecklace = TrinketUtils.isWearing(entity,
					github.com.gengyoubo.sscfa.init.AddonItems.ACTIVE_CORAL_NECKLACE.get());
		}
		if (hasNecklace) {
			entity.heal((float) BAL.d("necklace_heal_per_tick", NECKLACE_HEAL_PER_TICK));
			float cur = entity.getAbsorptionAmount();
			// Never remove absorption from another source when it already exceeds our cap.
			float room = Math.max(0f, (float) BAL.d("necklace_absorb_max", NECKLACE_ABSORB_MAX) - cur);
			float next = cur + Math.min(room, (float) BAL.d("necklace_absorb_per_tick", NECKLACE_ABSORB_PER_TICK));
			entity.setAbsorptionAmount(next);
			// 记录「装死给的黄心」增量，??30s 存留后衰减（仅这部分会衰减，其它来源不动???
			if (entity instanceof Player p) {
				github.com.gengyoubo.sscfa.ability.PlayDeadAbsorptionManager.addAbsorption(p, next - cur);
			}
		} else {
			entity.heal((float) BAL.d("heal_per_tick", DEFAULT_HEAL_PER_TICK));
		}
	}
}
