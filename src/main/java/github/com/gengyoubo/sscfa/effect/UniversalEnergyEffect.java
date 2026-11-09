package github.com.gengyoubo.sscfa.effect;

import github.com.gengyoubo.sscfa.item.UniversalEnergyPotionItem;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 通用能量药水的瞬时效果载体（jackcooper）：仅用于喷???/ 滞留型的原版投掷药水弹射物???
 *
 * <p>饮用型不走效果（直??finishUsing 服务端结算）；喷??/ 滞留型投掷时生成携带本效果的原??
 * {@code PotionEntity}，落?AOE 逐实体触?{@link #applyInstantenousEffect}，距离衰减后??
 * {@link UniversalEnergyPotionItem#canRestore}/{@code restoreMana} 同源逻辑回复?
 * 判定与数值完全复用饮用型静态方法，保证喷??/ 滞???/ 饮用三型行为一致??
 * 实现参照原??SSC {@code FeedEffect}（isInstant + applyInstantEffect + proximity 衰减）???
 */
public class UniversalEnergyEffect extends MobEffect {

	/** 瓶??/ 粒子着色：与饮用型贴图的青蓝色系一?*/
	public static final int POTION_COLOR = 0x3FD8D0;

	public UniversalEnergyEffect() {
		super(MobEffectCategory.BENEFICIAL, POTION_COLOR);
	}

	@Override
	public boolean isInstantenous() {
		return true;
	}

	@Override
	public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity attacker, LivingEntity target, int amplifier, double proximity) {
		// 仅服务端玩家可吸收能量（喷溅/滞???AOE 由原?PotionEntity 在服务端触发；与饮用型只认玩家同源）
		if (!(target instanceof net.minecraft.server.level.ServerPlayer player)) {
			return;
		}
		if (!UniversalEnergyPotionItem.canRestore(player)) {
			return;
		}
		// 距离衰减：爆炸中??1.0 ??边??0.0，下?0.5（与原???FeedEffect 同参）???
		// 只??scale，??restoreManaScaled 内部统一??MANA_RESTORE—??
		// 曾在此预乘过一次导致双重乘算??5??12.5~25)=312~625），滞留??喷溅一跳即溢出回满，勿再预乘??
		double scale = Math.max(0.5, proximity);
		UniversalEnergyPotionItem.restoreManaScaled(player, scale);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		// 瞬时效果：不???tick 结算（滞留云会以 instant 方式周期性调??applyInstantEffect??
		return false;
	}
}
