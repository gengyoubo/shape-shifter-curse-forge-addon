package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.ability.JumpKillManager;
import github.com.gengyoubo.sscfa.ability.NineLivesManager;
import github.com.gengyoubo.sscfa.ability.NovaSkillManager;
import github.com.gengyoubo.sscfa.ability.VortexChargeManager;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.server.level.ServerPlayer;

/**
 * SSCA 纯否决型伤害分支（jackcooper?026-09-09 ?SscAddonLivingEntityMixin 迁移?
 * 官方 {@link ServerLivingEntityEvents#ALLOW_DAMAGE}，先例：FluorescentDodgeHandler）?
 *
 * <p>只承接「纯否决」（返回 false = 取消本次伤害、不改数值不改伤害源）的分支?/p>
 * <ul>
 *   <li>跳蛛「跳杀」腾空期：免?b>已锁定目?/b>对自己的反打（其它来源照常受伤）?/li>
 *   <li>朔望九命：复活后 1s 无敌窗口内否决一切伤害；</li>
 *   <li>朔望「闪避」：概率完全免疫本次伤害（不受伤、不击退）?/li>
 * </ul>
 *
 * <p><b>注意</b>：朔望的「致死伤害触发复?+ 手动补击退」仍留在 mixin 内——复活必须发生在
 * damage 管线中段（血量结算前），ALLOW_DAMAGE ?HEAD 之前触发时机不同且手动补刀会重入事件链?
 * 「战斗标?/ 挑衅记录 / 金沙岚回血」等非否决逻辑同样留在?mixin（它们与伤害否决语义无关）?/p>
 */
public final class SscaDamageVetoHandler {

	private SscaDamageVetoHandler() {
	}

	public static void register() {
		github.com.gengyoubo.sscfa.util.SscEvents.allowDamage((entity, source, amount) -> {
			if (entity instanceof ServerPlayer caster && VortexChargeManager.suppressesNpcAttack(caster, source.getEntity())) {
				return false;
			}
			// 跳蛛跳杀腾空期免疫（仅服务端玩家、仅锁定目标来源?
			if (entity instanceof ServerPlayer sp
					&& JumpKillManager.isLeapingAgainst(sp, source.getEntity())) {
				return false;
			}
			// 朔望九命：复活无敌窗?+ 概率闪避
			if (entity instanceof ServerPlayer nova && FormUtils.isForm(nova, FormIdentifiers.OCELOT_NOVA)) {
				if (NineLivesManager.isInvulnerable(nova)) {
					return false;
				}
				if (NovaSkillManager.rollDodge(nova)) {
					return false; // 闪避：概率免疫本次伤害（不受伤、不击退?
				}
			}
			return true;
		});
	}
}
