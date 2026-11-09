package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.entity.SpellFireBoltEntity;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.spell.SpellRarity;

/**
 * 火球术（火系，白色基底，jackcooper）：朝准星射出一枚火球，命中造成魔法伤害并点燃目标。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/fire_bolt.json}：
 * 基准 5 伤 + 点燃 3s（L3+ 5s）/ cd 10s（L5 3s） / 耗蓝 15 / 单独惩罚 0.5×伤 2×cd；
 * 点燃时长不随伤害缩放（固定附加效果），L3 起提升到 5s。</p>
 */
public class FireBoltSpell extends Spell {

	@Override public double getProjectileRange(int level) { return SpellFireBoltEntity.maxDistance(); }

	/** L1-2 点燃 3s；L3+ 点燃 5s。默认值；运行时从 balance 快照读取。 */
	private static final int FIRE_TICKS_LOW = 60;
	private static final int FIRE_TICKS_HIGH = 100;

	// 阶段 5：运行时快照读取（spells.fire_bolt；快照未初始化回退上方默认常量）
	private static final BalanceReader BAL = new BalanceReader("spells.fire_bolt");

	public FireBoltSpell() {
		super(new ResourceLocation("ssc_addon", "fire_bolt"), SpellRarity.WHITE);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		SpellFireBoltEntity bolt = new SpellFireBoltEntity(caster.level(), caster);
		bolt.setDamage(power);
		bolt.setLevel(level);
		bolt.setExpBountyTen(ssc_addon$takePendingExp()); // exp_mode 1/2 挂起经验随弹射物走
		bolt.setRefundCastId(solo ? null : ssc_addon$getRefundCastId());
		// 低≤高跨字段约束由 balance schema 校验，运行时两值独立读取
		bolt.setRemainingFireTicks(RuneCastContext.current().duration(level >= 3
				? BAL.i("fire_ticks_high", FIRE_TICKS_HIGH)
				: BAL.i("fire_ticks_low", FIRE_TICKS_LOW),RuneModifiers.Stat.BURN));
		Vec3 look = caster.getViewVector(1.0F);
		bolt.setDirection(look, (float)RuneCastContext.current().scale(getSpeedMultiplier(level),RuneModifiers.Stat.SPEED));
		caster.level().addFreshEntity(bolt);
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
	}
}
