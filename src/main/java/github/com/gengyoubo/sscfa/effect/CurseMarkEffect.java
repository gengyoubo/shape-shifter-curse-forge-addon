package github.com.gengyoubo.sscfa.effect;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 诅咒标记（jackcooper，诅咒系法术）：标记期间受到的所有伤害加深（每??+10%???=+20% ??L5=+60%）??
 * 实际伤害加深???{@code SscAddonLivingEntityMixin} ??damage ModifyVariable 拦截实??
 * （带此效果??amount ??(1.2 + 0.1×amplifier)；amplifier 由施法时按等级写入，L1=0 ??L5=4）??
 * 本类只作可视化标记（粒???+ 图标）???
 *
 * <p>可被月辉系净化（月华治愈移除负面效果时一并清除——HARMFUL 类别天然可清）?/p>
 */
public class CurseMarkEffect extends MobEffect {

	/** 每级额外加深幅度（??mixin 内保持一致；基础 +20% 在两者内同源）?*/
	public static final float BONUS_PER_LEVEL = 0.1f;
	/** 基础加深幅度?? ??+20%）??*/
	public static final float BASE_BONUS = 0.2f;
	/** 第二段：被标记者输出削弱基础幅度?? = -15%）??*/
	public static final float OUTPUT_WEAKEN_BASE = 0.15f;
	/** 第二段：输出削弱每级增量?? = -15% - 4??.0875 = -50%，线性内插）??*/
	public static final float OUTPUT_WEAKEN_PER_LEVEL = 0.0875f;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量；供本类??SscAddonLivingEntityMixin 共用?
	private static final BalanceReader BAL = new BalanceReader("abilities.curse_mark");

	/** 受伤加深基础幅度（balance 快照读取）??*/
	public static float takenBase() {
		return (float) BAL.d("taken_base", BASE_BONUS);
	}

	/** 受伤加深每级增量（balance 快照读取）??*/
	public static float takenPerLevel() {
		return (float) BAL.d("taken_per_level", BONUS_PER_LEVEL);
	}

	/** 输出削弱基础幅度（balance 快照读取）??*/
	public static float outputWeakenBase() {
		return (float) BAL.d("output_weaken_base", OUTPUT_WEAKEN_BASE);
	}

	/** 输出削弱每级增量（balance 快照读取）??*/
	public static float outputWeakenPerLevel() {
		return (float) BAL.d("output_weaken_per_level", OUTPUT_WEAKEN_PER_LEVEL);
	}

	/** 按等级计算总加深倍率??=1.2 ??L5=1.6）??*/
	public static float multiplierForLevel(int level) {
		return 1.0f + takenBase() + takenPerLevel() * (Math.max(1, Math.min(5, level)) - 1);
	}

	public CurseMarkEffect() {
		// 暗紫主题色（与诅咒??FormationElement.CURSE 0x7B2FBE 一致??
		super(MobEffectCategory.HARMFUL, 0x7B2FBE);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// 周期粒子：暗紫咒纹环绕（服务端撒，天然多人同步）
		if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverWorld
				&& entity.tickCount % 20 == 0) {
			serverWorld.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
					entity.getX(), entity.getY(0.5), entity.getZ(), 2, 0.3, 0.4, 0.3, 0.01);
		}
	}
}
