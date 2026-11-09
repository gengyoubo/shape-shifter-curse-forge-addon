package github.com.gengyoubo.sscfa.spell.spells;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import java.util.List;

/**
 * 冰霜新星（冰系，绿色基底，jackcooper）：以自身为圆心爆发寒气，范围内造成伤??+ 缓??II 4s??
 *
 * <p>数值外?{@code data/ssc_addon/spells/frost_nova.json}??
 * 基??3 ??/ 半??4 ??/ cd 5s / 耗??15；半径按 speed_multiplier 缩放（每??+0.5 格）?
 * 稀有度为蓝/橙时额外 +25%??
 * 纯控制向：不点燃（对比烈焰新星），缓速时长随等级微涨（L1 4s ??L5 6s）??
 * 白名单：主人在线且目标受保????免伤；施法者本人不受影响?/p>
 */
public class FrostNovaSpell extends Spell {

	/** 基础半径（格）默认，实际半径 = 基????speed_multiplier(level)；运行时??balance 快照读取??*/
	private static final double BASE_RADIUS = 4.0;

	// 阶??5：运行时快照读取（spells.frost_nova；快照未初始化回退上方默认常量??
	private static final BalanceReader BAL = new BalanceReader("spells.frost_nova");

	public FrostNovaSpell() {
		super(new ResourceLocation("ssc_addon", "frost_nova"), SpellRarity.GREEN);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		if (!(caster.level() instanceof ServerLevel serverWorld)) {
			return;
		}
		double radius = RuneCastContext.current().scale(BAL.d("base_radius", BASE_RADIUS) * getSpeedMultiplier(level),RuneModifiers.Stat.AREA);
		// 稀有度为蓝/橙时，生效范围额?+25%（独立于等级缩放，数据包?rarity 自动跟随??
		SpellRarity rarity = getRarity(level);
		if (rarity == SpellRarity.BLUE || rarity == SpellRarity.ORANGE) {
			radius *= 1.25;
		}
		// 缂傛捇鈧?II閿涘本妞傞梹?4s閿涘湢1-2閿? 5s閿涘湢3-4閿? 6s閿涘湢5閿?
		int slowTicks = RuneCastContext.current().duration(80 + (level >= 3 ? 20 : 0) + (level >= 5 ? 20 : 0),RuneModifiers.Stat.NEGATIVE);
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				caster.getBoundingBox().inflate(radius), e -> e != caster && e.isAlive());
		int hitExpTen = ssc_addon$takePendingExp();
		for (LivingEntity target : targets) {
			if (target.distanceTo(caster) > radius) {
				continue;
			}
			// 公共命中结算（白名单豁免 ??法术伤害 ??经验补发 ??流派钩子逐目标；ICE 系无击杀分支）：??SpellHitHelper
			var hit = github.com.gengyoubo.sscfa.spell.SpellHitHelper.projectileHit(
					caster, target, power, github.com.gengyoubo.sscfa.spell.FormationElement.ICE,
					solo ? null : ssc_addon$getRefundCastId(), hitExpTen);
			if (hit != github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				continue;
			}
			hitExpTen = 0; // Only the first successful hit consumes the cast budget.
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
			// 霜碎：护??-50%，时长与缓速同步（L1=3s ??L5=5s??
			target.addEffect(new MobEffectInstance(
					github.com.gengyoubo.sscfa.init.AddonEffects.FROST_SHATTER.get(), slowTicks, 0));
		}
		// 演出：球形寒气粒子（双层雪花球面 + 内部云雾?? 寒气音效（与烈焰新星同款球形演出??
		github.com.gengyoubo.sscfa.util.SpellFxUtils.sphere(serverWorld, caster,
				caster.getX(), caster.getY() + 1.0, caster.getZ(), radius,
				ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.CLOUD, 8);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 0.8f);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
	}
}
