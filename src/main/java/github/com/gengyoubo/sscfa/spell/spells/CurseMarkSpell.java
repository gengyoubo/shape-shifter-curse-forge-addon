package github.com.gengyoubo.sscfa.spell.spells;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.entity.SpellCurseMarkEntity;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;

/**
 * 诅咒标记（诅咒系，绿色基底，jackcooper）：朝准星射出诅咒印记，命中目标后
 * 8 秒内受到的伤害 +20%（伤害加深），且其造成的伤害 -15%（双段削弱）；可被月辉系净化。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/curse_mark.json}：
 * 基准 0 直伤 / 8s 标记（受伤 +20%）/ cd 15s（L5 10s，每级 -1.25s）/ 耗蓝 15；标记时长按等级 +2s/级（L5=16s）。</p>
 */
public class CurseMarkSpell extends Spell {

	@Override public double getProjectileRange(int level) { return SpellCurseMarkEntity.maxDistance(); }

	public CurseMarkSpell() {
		super(new ResourceLocation("ssc_addon", "curse_mark"), SpellRarity.GREEN);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		// 标记时长：8s + 2s/级（L1=8s、L3=12s、L5=16s）
		int durationTicks = 160 + (level - 1) * 40;
		if (!solo) durationTicks = github.com.gengyoubo.sscfa.spell.FormAffinity.curseDurationTicks(caster, durationTicks);
		SpellCurseMarkEntity mark = new SpellCurseMarkEntity(caster.level(), caster);
		mark.setDuration(RuneCastContext.current().duration(durationTicks,RuneModifiers.Stat.MARK,RuneModifiers.Stat.NEGATIVE));
		mark.setLevel(level);
		mark.setRefundCastId(solo ? null : ssc_addon$getRefundCastId());
		Vec3 look = caster.getViewVector(1.0F);
		mark.setDirection(look,(float)RuneCastContext.current().scale(getSpeedMultiplier(level),RuneModifiers.Stat.SPEED));
		caster.level().addFreshEntity(mark);
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 0.6f);
	}
}
