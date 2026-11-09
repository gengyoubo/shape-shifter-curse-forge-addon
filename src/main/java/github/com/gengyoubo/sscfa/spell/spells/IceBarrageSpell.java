package github.com.gengyoubo.sscfa.spell.spells;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.entity.SpellFrostSpikeEntity;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;

/**
 * 冰锥齐射（冰系，绿色基底，jackcooper）：朝准星扇形散射 3 枚可穿刺冰锥，各自独立命中结算。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/ice_barrage.json}：
 * 基准每枚 4 伤 / cd 15s（L5 8s） / 耗蓝 30；三枚呈 ±12° 扇形，速度按等级倍率缩放（Lv5 ×1.75）。
 * <b>穿刺</b>：每枚最多可命中 2 + (等级-1) 个敌人（Lv1=2 → Lv5=6），命中后不碎裂继续飞行，
 * 达上限或撞方块/超距才消失（穿刺机制在实体内实现，单发冰锥不受影响）。
 * 复用 {@link SpellFrostSpikeEntity}（伤害即 power），不新建实体。</p>
 */
public class IceBarrageSpell extends Spell {

	@Override public double getProjectileRange(int level) { return SpellFrostSpikeEntity.maxDistance(); }

	/** 散射枚数。默认值；运行时从 balance 快照读取。 */
	private static final int COUNT = 3;
	/** 扇形半角（度）：三枚 = 中心 1 枚 + 两侧各 1 枚偏转此角度。默认值；运行时从 balance 快照读取。 */
	private static final float SPREAD_DEG = 12.0f;
	/** 基础穿刺数（单枚最多命中敌人数），每级 +1。默认值；运行时从 balance 快照读取。 */
	private static final int PIERCE_BASE = 2;

	// 阶段 5：运行时快照读取（spells.ice_barrage；快照未初始化回退上方默认常量）
	private static final BalanceReader BAL = new BalanceReader("spells.ice_barrage");

	public IceBarrageSpell() {
		super(new ResourceLocation("ssc_addon", "ice_barrage"), SpellRarity.GREEN);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		Vec3 look = caster.getViewVector(1.0F);
		float speedMul = (float)RuneCastContext.current().scale(getSpeedMultiplier(level),RuneModifiers.Stat.SPEED);
		// 运行时快照读取（同方法多次使用的参数先取局部变量）
		int count = BAL.i("count", COUNT);
		float spreadDeg = (float)RuneCastContext.current().scale(BAL.d("spread_deg", SPREAD_DEG),RuneModifiers.Stat.AREA);
		// exp 挂起经验均分给三枚（bounty 为绝对值，cast 时一次性取走除以枚数）
		int bountyTen = ssc_addon$takePendingExp();
		int eachBounty = bountyTen / count;
		java.util.UUID castId = solo ? null : ssc_addon$getRefundCastId();
		// 按 count 均匀铺开扇形：count=3 时即中心 1 枚 + 两侧各 1 枚偏 spreadDeg
		for (int i = 0; i < count; i++) {
			float yawOffset = (i - (count - 1) * 0.5f) * spreadDeg;
			SpellFrostSpikeEntity spike = new SpellFrostSpikeEntity(caster.level(), caster);
			spike.setDamage(power);
			spike.setLevel(level); // 真实魔法等级：L4+ 渲染 3D 冰锥模型（与「冰锥」法术同款外观）
			spike.setPierceCount(BAL.i("pierce_base", PIERCE_BASE) + (level - 1)); // 穿刺数：Lv1=2 → Lv5=6（单枚最多命中敌人数）
			spike.setExpBountyTen(eachBounty + (i < bountyTen % count ? 1 : 0));
			spike.setRefundCastId(castId);
			Vec3 dir = rotateYaw(look, yawOffset);
			spike.setDirection(dir, speedMul);
			caster.level().addFreshEntity(spike);
		}
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0f, 1.3f);
	}

	/** 把方向向量绕 Y 轴旋转 deg 度（水平偏转，保持准星俯仰）。 */
	private static Vec3 rotateYaw(Vec3 v, float deg) {
		if (deg == 0f) {
			return v;
		}
		double rad = Math.toRadians(deg);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		return new Vec3(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
	}
}
