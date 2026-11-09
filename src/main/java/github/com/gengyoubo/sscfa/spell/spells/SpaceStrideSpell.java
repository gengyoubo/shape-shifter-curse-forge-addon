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
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;

/**
 * 空间漫步（空间系，白色基底，jackcooper）：探索向群体增益——缓降 20s +
 * 跳跃提升 II 10s（自己；随等级延长）。纯机动辅助，无伤害。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/space_stride.json}：
 * 基准缓降 20s / 跳升 II 10s / cd 20s / 耗蓝 12；时长按等级递增。</p>
 */
public class SpaceStrideSpell extends Spell {

	/** 基础跳升时长（tick）默认：10s；缓降 = 跳升 + 2s（2026-09-19 用户定稿，节奏对齐）；运行时从 balance 快照读取。 */
	private static final int JUMP_TICKS = 200;
	/** 缓降与跳升的固定差值（tick）默认：2s；运行时从 balance 快照读取。 */
	private static final int SLOW_FALL_EXTRA_TICKS = 40;

	// 阶段 5：服务端权威快照读取（spells.space_stride；快照未初始化回退默认常量）
	private static final BalanceReader BAL = new BalanceReader("spells.space_stride");

	public SpaceStrideSpell() {
		super(new ResourceLocation("ssc_addon", "space_stride"), SpellRarity.WHITE);
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
		int jump = BAL.i("jump_ticks", JUMP_TICKS) + (level - 1) * 50;
		int slowFall = jump + BAL.i("slow_fall_extra_ticks", SLOW_FALL_EXTRA_TICKS); // 缓降恒比跳升多 2s（随等级同步递增）
		// 跳升每两级 +1 级：L1/L2=I、L3/L4=II、L5=III（amplifier = 级数-1）
		jump=RuneCastContext.current().duration(jump,RuneModifiers.Stat.ALLY_DURATION);
        slowFall=RuneCastContext.current().duration(slowFall,RuneModifiers.Stat.ALLY_DURATION);
        int jumpAmplifier = (level - 1) / 2;
		caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, slowFall, 0));
		caster.addEffect(new MobEffectInstance(MobEffects.JUMP, jump, jumpAmplifier));
		// 演出：脚下星尘环绕
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, caster, ParticleTypes.CLOUD,
				caster.getX(), caster.getY() + 0.2, caster.getZ(), 8, 0.4, 0.1, 0.4, 0.02);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, caster, ParticleTypes.END_ROD,
				caster.getX(), caster.getY() + 1.0, caster.getZ(), 10, 0.3, 0.5, 0.3, 0.03);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.RABBIT_JUMP, SoundSource.PLAYERS, 0.8f, 1.2f);
	}

}
