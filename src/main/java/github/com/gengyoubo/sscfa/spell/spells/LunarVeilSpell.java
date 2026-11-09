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
 * 月幕（月辉系，绿色基底，jackcooper）：以自身为圆心展开月光辉幕，范围内友方
 * （自己 + 未受白名单保护的玩家/驯服宠物）获得抗性 + 缓降，持续一段时间的即时群体增益。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/lunar_veil.json}：
 * 基准半径 3 格 / 抗性 I 8s + 缓降 I 8s / cd 25s（L5 15s） / 耗蓝 25；
 * 半径按 speed_multiplier 缩放（与新星家族一致）。</p>
 *
 * <p>白名单语义：受保护目标（白名单内玩家与宠物）同样受益——增益类法术不做排除。</p>
 */
public class LunarVeilSpell extends Spell {

	/** 基础半径（格）默认，实际半径 = 基础 × speed_multiplier(level)：L1=4 → L5=7（每级 +0.75）；运行时从 balance 快照读取。 */
	private static final double BASE_RADIUS = 4.0;
	/** 增益时长（tick）默认：8s；运行时从 balance 快照读取。 */
	private static final int DURATION_TICKS = 160;

	// 阶段 5：服务端权威快照读取（spells.lunar_veil；快照未初始化回退默认常量）
	private static final BalanceReader BAL = new BalanceReader("spells.lunar_veil");

	public LunarVeilSpell() {
		super(new ResourceLocation("ssc_addon", "lunar_veil"), SpellRarity.GREEN);
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
		// 增益时长：每级 +1s（L1=9s … L5=13s）；L4+ 增益升 II 级（抗性 II + 缓降 II）
		int duration = RuneCastContext.current().duration(BAL.i("duration_ticks", DURATION_TICKS) + (level - 1) * 20,RuneModifiers.Stat.ALLY_DURATION);
		int amplifier = level >= 4 ? 1 : 0;
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				caster.getBoundingBox().inflate(radius), e -> e != caster && e.isAlive());
		for (LivingEntity target : targets) {
			if (target.distanceTo(caster) > radius) {
				continue;
			}
			// 增益目标筛选（统一白名单判定）：玩家、驯服宠物、以及 /ssc_addon whitelist add
			// 加入白名单的 NPC 生物均受益（与 SP 悦灵群体治疗同款 WhitelistUtils.isBuffTarget 语义）
			if (!github.com.gengyoubo.sscfa.util.WhitelistUtils.isBuffTarget(caster, target)) {
				continue;
			}
			target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier));
			target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, amplifier));
			serverWorld.sendParticles(ParticleTypes.END_ROD,
					target.getX(), target.getY(0.8), target.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
		}
		// 自己也享受
		caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier));
		caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, amplifier));
		// 演出：月幕粒子环 + 空灵音效
		github.com.gengyoubo.sscfa.util.SpellFxUtils.ring(serverWorld, caster, ParticleTypes.END_ROD,
				caster.getX(), caster.getY() + 0.2, caster.getZ(), radius, 24);
		github.com.gengyoubo.sscfa.util.SpellFxUtils.ring(serverWorld, caster, ParticleTypes.CLOUD,
				caster.getX(), caster.getY() + 0.5, caster.getZ(), radius * 0.7, 12);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.6f);
	}
}
