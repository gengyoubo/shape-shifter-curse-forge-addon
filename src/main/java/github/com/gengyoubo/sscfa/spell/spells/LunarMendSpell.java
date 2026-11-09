package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.List;

/**
 * 月华治愈（月辉系，白色基底，jackcooper）：净化自身——回复生命 + 移除一个负面状态效果。
 * 无伤害魔法，power 语义 = 回复生命值。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/lunar_mend.json}：
 * 基准回 5 HP / cd 12s / 耗蓝 20；L3 起额外移除第二个负面效果。</p>
 */
public class LunarMendSpell extends Spell {

	public LunarMendSpell() {
		super(new ResourceLocation("ssc_addon", "lunar_mend"), SpellRarity.WHITE);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		// 蝙蝠「血契」配套：月华治愈效果 +25%（吸血蝙蝠主题，2026-09-17）
		power = github.com.gengyoubo.sscfa.spell.FormCastingStyle.lunarMendBonus(caster, power);
		// 回复生命（不含再生，即时治疗语义）
		if (github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.reversed()) {
            caster.hurt(caster.damageSources().magic(), power);
            return;
        }
        github.com.gengyoubo.sscfa.spell.FormSpellStyle.heal(caster,caster,power);
		// 净化预算制（2026-09-17）：等级 = 预算点数（L1=1 … L5=5）；
		// 移除一个 amplifier=n 的负面效果消耗 n+1 点（一级 debuff 消耗 1，二级消耗 2，以此类推）；
		// 预算不足以移除下一个（如剩 1 点遇到二级）则跳过继续找可移除的，预算耗尽停止。
		int budget = level;
		for (MobEffectInstance instance : List.copyOf(caster.getActiveEffects())) {
			if (budget <= 0) {
				break;
			}
			if (instance.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
				int cost = instance.getAmplifier() + 1;
				if (cost <= budget) {
					caster.removeEffect(instance.getEffect());
					budget -= cost;
				}
				// 预算不够（如剩 1 遇到二级）：跳过找更便宜的，不停止整个净化
			}
		}
		// 演出：月光洒落 + 治愈音效
		if (caster.level() instanceof ServerLevel serverWorld) {
			serverWorld.sendParticles(ParticleTypes.END_ROD,
					caster.getX(), caster.getY() + 1.2, caster.getZ(), 20, 0.4, 0.6, 0.4, 0.03);
			serverWorld.sendParticles(ParticleTypes.HEART,
					caster.getX(), caster.getY() + 1.0, caster.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
			serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.6f);
		}
	}

}
