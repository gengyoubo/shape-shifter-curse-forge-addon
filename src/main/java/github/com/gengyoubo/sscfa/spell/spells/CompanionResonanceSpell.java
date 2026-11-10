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
import net.minecraft.world.entity.TamableAnimal;
import github.com.gengyoubo.sscfa.ability.CompanionResonanceManager;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.entity.LunarSpiritEntity;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import java.util.List;

/**
 * 伙伴共鸣（召唤系，白色基底，jackcooper）：强化自己的宠物（狼/猫/鹦鹉等驯服生物）
 * 与自己的召唤物（月灵等归属自己的友方实体）。增益类：白名单内同样受益。
 *
 * <p>增益按等级成长：</p>
 * <ul>
 *   <li>伤害：每级 +2 点（属性修饰符，固定 UUID 到期移除，见
 *       {@link CompanionResonanceManager}）；</li>
 *   <li>迅捷：每两级 +1 级（L1=迅捷 I、L3=迅捷 II、L5=迅捷 III）；</li>
 *   <li>抗性：L3 起每两级 +1 级（L3/L4=抗性 I、L5=抗性 II）。</li>
 * </ul>
 *
 * <p>数值外置 {@code data/ssc_addon/spells/companion_resonance.json}：
 * 基准半径 8 格 / 30s + 10s/级 / cd 25s（L5 15s，每级 -2.5s）/ 耗蓝 15。</p>
 */
public class CompanionResonanceSpell extends Spell {

	/** 增益半径（格）默认；运行时从 balance 快照读取。 */
	private static final double RADIUS = 8.0;
	/** 基础时长（tick）默认：30s；运行时从 balance 快照读取。 */
	private static final int BASE_DURATION_TICKS = 600;
	/** 每级增加的时长（tick）默认：10s；运行时从 balance 快照读取。 */
	private static final int DURATION_PER_LEVEL = 200;

	// 阶段 5：服务端权威快照读取（spells.companion_resonance；快照未初始化回退默认常量）
	private static final BalanceReader BAL = new BalanceReader("spells.companion_resonance");

	public CompanionResonanceSpell() {
		super(new ResourceLocation("ssc_addon", "companion_resonance"), SpellRarity.WHITE);
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
		int duration = BAL.i("base_duration_ticks", BASE_DURATION_TICKS)
				+ (level - 1) * BAL.i("duration_per_level", DURATION_PER_LEVEL);
		// 状态效果等级：迅捷每两级 +1（L1=0、L3=1、L5=2）；抗性 L3 起每两级 +1（L3=0、L5=1）
		int speedAmplifier = (level - 1) / 2;
		int resistanceAmplifier = level >= 3 ? (level - 3) / 2 : -1; // -1 = 不加抗性
		duration=RuneCastContext.current().duration(duration,RuneModifiers.Stat.ALLY_DURATION);
        double radius = RuneCastContext.current().scale(BAL.d("radius", RADIUS),RuneModifiers.Stat.AREA); // 同方法多处使用，读一次局部化
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				caster.getBoundingBox().inflate(radius), e -> e != caster && e.isAlive());
		int buffed = 0;
		for (LivingEntity target : targets) {
			if (target.distanceTo(caster) > radius) {
				continue;
			}
			// 目标筛选：自己的驯服宠物 或 自己的召唤物（月灵）
			boolean isOwnPet = target instanceof TamableAnimal tameable
					&& tameable.isTame() && tameable.getOwner() == caster;
			boolean isOwnSummon = target instanceof LunarSpiritEntity spirit
					&& caster.getUUID().equals(spirit.getOwnerUuid());
			if (!isOwnPet && !isOwnSummon) {
				continue;
			}
			// 伤害：每级 +2（属性修饰符，到期移除）
			CompanionResonanceManager.apply(target, level, duration);
			// 迅捷：每两级 +1 级
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, speedAmplifier));
			// 抗性：L3 起每两级 +1 级
			if (resistanceAmplifier >= 0) {
				target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resistanceAmplifier));
			}
			buffed++;
			// 单体演出：绿色共鸣上升粒子
			serverWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					target.getX(), target.getY(0.6), target.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
		}
		// 施法者反馈（无目标时也提示，避免「无反应」困惑）
		if (buffed == 0) {
			caster.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.ssc_addon.spell.companion_none")
					.withStyle(net.minecraft.ChatFormatting.YELLOW), true);
		} else {
			caster.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.ssc_addon.spell.companion_buffed", buffed)
					.withStyle(net.minecraft.ChatFormatting.GREEN), true);
		}
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.8f);
	}
}
