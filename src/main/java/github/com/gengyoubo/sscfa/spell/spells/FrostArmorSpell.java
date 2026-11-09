package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import github.com.gengyoubo.sscfa.spell.FrostArmorAccess;

/**
 * 霜甲术（冰系，蓝色基底，jackcooper）：给自己施加「伤害吸收」（黄心）—?
 * 无伤害魔法，power 语??= 期望吸收点数?
 *
 * <p>独立霜甲效果计时，吸收池补??{@code max(current, power)}，重复施放不叠加?
 * 精确记录本法术补入的份额，受伤先消耗霜甲；到期或效果被移除时只移除霜甲剩余的份额??
 * 不改写金苹果等其他吸收效果的等级和持续时间??/p>
 *
 * <p>数值外?{@code data/ssc_addon/spells/frost_armor.json}??
 * 基??8 吸收? 黄心? 持??20s / cd 25s 每??-2.5s / 耗??25 每????.2（复利，与月辉诅咒系 buff 类一致）??
 * L1-L5 = 吸??8/12/16/20/24 点??/6/8/10/12 黄心），耗??25/30/36/43/52，cd 25/22.5/20/17.5/15s??
 * 重复施放刷新时长（吸收量??max，不叠加）??/p>
 */
public class FrostArmorSpell extends Spell {

	/** 吸收持续时间（tick）??0s。默认值；运行时从 balance 快照读取??*/
	private static final int DURATION_TICKS = 400;

	// 阶??5：运行时快照读取（spells.frost_armor；快照未初始化回退上方默认常量??
	// 持续时间和吸收量均不借用原版吸收效果的整数档位）
	private static final BalanceReader BAL = new BalanceReader("spells.frost_armor");

	public FrostArmorSpell() {
		super(new ResourceLocation("ssc_addon", "frost_armor"), SpellRarity.BLUE);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		caster.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.FROST_ARMOR.get(),
				RuneCastContext.current().duration(BAL.i("duration_ticks", DURATION_TICKS),RuneModifiers.Stat.SHIELD_DURATION), 0));
		if (caster.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.FROST_ARMOR.get())) ((FrostArmorAccess) caster).ssca$frostArmor().grant(caster, power);
		// 演出：寒气缠?+ 冰晶盾碎裂音?
		if (caster.level() instanceof ServerLevel serverWorld) {
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, caster, ParticleTypes.SNOWFLAKE,
					caster.getX(), caster.getY() + 1.0, caster.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
			serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 1.6f);
			serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.8f, 1.4f);
		}
	}

}
