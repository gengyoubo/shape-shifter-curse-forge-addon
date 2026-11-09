package github.com.gengyoubo.sscfa.spell.spells;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.entity.SpellMoonlightArrowEntity;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;

/**
 * 月光箭（月辉系，绿色基底，jackcooper）：朝准星射出一支月光箭，
 * 命中造成魔法伤害；对亡灵生物额外 +50%。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/moonlight_arrow.json}：
 * 基准 4 伤（对亡灵 6）/ cd 10s（L5 5s） / 耗蓝 12；速度按 speed_multiplier 缩放（弹道更快）。</p>
 */
public class MoonlightArrowSpell extends Spell {

	@Override public double getProjectileRange(int level) { return SpellMoonlightArrowEntity.maxDistance(); }

	public MoonlightArrowSpell() {
		super(new ResourceLocation("ssc_addon", "moonlight_arrow"), SpellRarity.GREEN);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		SpellMoonlightArrowEntity arrow = new SpellMoonlightArrowEntity(caster.level(), caster);
		arrow.setDamage(power);
		arrow.setLevel(level);
		arrow.setExpBountyTen(ssc_addon$takePendingExp());
		arrow.setRefundCastId(solo ? null : ssc_addon$getRefundCastId());
		Vec3 look = caster.getViewVector(1.0F);
		arrow.setDirection(look,(float)RuneCastContext.current().scale(getSpeedMultiplier(level),RuneModifiers.Stat.SPEED));
		caster.level().addFreshEntity(arrow);
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.0f);
		// Amethyst shimmer has a quiet material gain; give release a clear crystal attack.
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.8f);
	}
}
