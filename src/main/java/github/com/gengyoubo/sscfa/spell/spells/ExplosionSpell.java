package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.spell.ExplosionManager;
import github.com.gengyoubo.sscfa.spell.ExplosionRules;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellCastingRules;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 爆裂魔法（烈焰系红色单级，jackcooper，2026-09-22 用户定稿）：
 * 按住施法键仅本地显示落点指针；松手后服务端锁定 128 格内的合法落点，
 * 才开始 35 秒禁动蓄力。落点法阵立即出现，第 3 秒升柱、第 11 秒到顶保持，
 * 第 34.5 秒长出红白球，第 35 秒由完成的施法结算爆炸，含施法者与友军。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/explosion.json}：300 蓝 / 3 分钟 CD /
 * custom 档 700t 禁动 / interrupt 3（同领域规格）。落点射线检测必须命中方块
 * （指天拒绝施放）。保留施法者脚下法阵，取消头顶法阵；打断同时移除落点演出。</p>
 */
public class ExplosionSpell extends Spell {
	@Override
	public boolean requiresTargetBeforeChannel() { return true; }

	@Override
	public void onChannelStarted(ServerPlayer caster, Vec3 target) {
		ExplosionManager.beginCharge(caster, target);
	}

	@Override
	public void onChannelEnded(ServerPlayer caster, boolean interrupted) {
		if (interrupted) ExplosionManager.cancelCharge(caster);
	}

	/** 位移打断（2026-09-22 用户定稿，同领域）：离蓄力锦点超 3 格 → 统一读条打断路径
	 * （stop(true)：返还 20% CD、清演出、取消禁动），而非仅静默移除序列。 */
	@Override
	public boolean canContinueCasting(ServerPlayer caster, net.minecraft.world.item.ItemStack scroll) {
		return ExplosionManager.isWithinChargeAnchor(caster);
	}
	/** 锁定点（2026-09-24 用户定稿二次调整）：主题音频起播（蓄力 540t / 第 27 秒 T-8s）后
	 * 伤害/主动取消/长按取消/位移均不再断（原为 682t 红白球生成）；净化仍可强制打断。 */
	@Override
	public boolean isLockedIn(ServerPlayer caster) {
		return ExplosionManager.isBallCharging(caster);
	}

	/** 锁定转换 tick（客户端 HUD 本地预测红显兜底，2026-09-24）：与服务端锁定阈值同源。
	 * 2026-09-24 用户定稿：锁定起点从红白球生成（682t）提前到主题音频起播（540t / 第 27 秒）
	 * ——音频响起即不可打断，红字从倒数第 8 秒开始显示。 */
	@Override
	public int getLockInTick() {
		return ExplosionRules.SOUND_START_TICKS;
	}

	public ExplosionSpell() {
		super(new ResourceLocation("ssc_addon", "explosion"), SpellRarity.RED);
	}

	/** 按住选点，最远 128 格；该阶段不创建施法会话。 */
	@Override
	public double getAimMaxRange() {
		return ExplosionRules.AIM_RANGE;
	}

	/** 使用独立几何指针，不显示通用粒子范围圈。 */
	@Override
	public double getAimRadius(int level) {
		return 0;
	}

	/** custom 档：35 秒（700t）完全禁动蓄力（同领域表现）。 */
	@Override
	protected SpellCastingRules.Profile getCustomCastingProfile(ServerPlayer caster, int level, boolean solo) {
		return new SpellCastingRules.Profile(ExplosionRules.CHARGE_TICKS, 0, true);
	}

	/** 领域内禁放爆裂魔法（2026-10-21 用户定稿，逐法术开关之一）。 */
	@Override
	public boolean isForbiddenInsideDomain() {
		return true;
	}

	/** 施法前置校验：落点必须命中方块（指天/超距无方块 → 拒绝施放，不耗法力/CD）。 */
	@Override
	public boolean canCast(ServerPlayer caster) {
		return Spell.computeAimImpact(caster, ExplosionRules.AIM_RANGE) != null;
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		Vec3 impact = getCastTarget(caster, level);
		if (impact == null) {
			return; // 锁定落点失效（理论不可达：松手时已校验），安全终止
		}
		// 演出已在蓄力中完成；此处立即结算，不再追加等待时间。
		ExplosionManager.detonate(caster, impact, power, solo ? null : ssc_addon$getRefundCastId());
	}
}
