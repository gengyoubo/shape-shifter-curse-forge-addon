package github.com.gengyoubo.sscfa.effect;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

/**
 * 蛛网缠身：非白名单生物踩过月织蛛的减速蛛网（web_membrane）时施加，或被蛛网弹爆炸范围波及时直接施加??
 * <ul>
 *   <li>大幅降低移速（-50%，MULTIPLY_TOTAL，属性修饰不显示图标）??/li>
 *   <li>挖掘疲劳 + 虚弱：???effect 内持续刷新（vanilla 效果，随蜘网缠身联动，三者同进退）???/li>
 *   <li>喝牛奶不可解除（?{@code MilkBucketWebBoundMixin}）??/li>
 *   <li>不在任何形态??buff 免疫列表里（使魔等亦挡不住）?/li>
 *   <li>持续期间脚下持续冒蛛网碎屑粒子，形成「脚上覆盖一层蛛网」的视觉??/li>
 * </ul>
 */
public class SpiderWebBoundEffect extends MobEffect {

	/** 联动的挖掘疲???虚弱持续时长（tick）：略大于刷新间??4t，保证无缝衔接?*/
	private static final int SUB_DURATION = 40;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.web_bound");

	public SpiderWebBoundEffect() {
		super(MobEffectCategory.HARMFUL, 0xBFC4CC);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
				"6C9E2A1F-8B3D-4A7E-9F21-3D5C7A0B1E44", -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return duration % 4 == 0; // ??4 tick 冒蛛网粒??+ 刷新挖掘疲??虚??
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.level() instanceof ServerLevel sw) {
			double spread = Math.max(0.15, entity.getBbWidth() * 0.35);
			sw.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()),
					entity.getX(), entity.getY() + 0.1, entity.getZ(),
					3, spread, 0.06, spread, 0.0);
		}
		// 挖掘疲劳 + 虚弱：放在蜘网缠身内联动，随蜘网缠身同进退（环???ambient=false，粒??false??
		int subDuration = BAL.i("sub_duration", SUB_DURATION);
		entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, subDuration, 0, false, false, false));
		entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, subDuration, 0, false, false, false));
	}
}
