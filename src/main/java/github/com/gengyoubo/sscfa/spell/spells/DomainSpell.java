package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.spell.DomainRules;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellCastingRules;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class DomainSpell extends Spell {
	public DomainSpell() { super(new ResourceLocation("ssc_addon", "domain"), SpellRarity.RED); }
	@Override
	protected SpellCastingRules.Profile getCustomCastingProfile(ServerPlayer caster, int level, boolean solo) {
		return new SpellCastingRules.Profile(DomainRules.chargeTicks(), 0, true);
	}
	@Override
	public boolean canCast(ServerPlayer caster) { return DomainManager.canStart(caster); }
	@Override
	public net.minecraft.network.chat.Component getCastFailureMessage(ServerPlayer caster) {
		return net.minecraft.network.chat.Component.translatable("message.ssc_addon.spellbook.domain_conflict");
	}
	@Override
	public void onChannelStarted(ServerPlayer caster) { DomainManager.begin(caster); }
	@Override
	public boolean canContinueCasting(ServerPlayer caster, ItemStack scroll) { return DomainManager.canContinue(caster); }
	/** 锁定点（2026-09-23 用户定稿）：黑色壳开始扩张（蓄力 200t / 第 10 秒）后不可打断，
	 * 伤害/主动取消/长按取消/位移走超 3 格均不再断；净化仍通过统一入口强制打断。 */
	@Override
	public boolean isLockedIn(ServerPlayer caster) {
		return DomainManager.isExpanding(caster);
	}

	/** 锁定转换 tick（客户端 HUD 本地预测红显兜底，2026-09-24）：与服务端 isExpanding
	 * 的阈值同源（DomainRules.expandStartTick()），勿单独写数。 */
	@Override
	public int getLockInTick() {
		return DomainRules.expandStartTick();
	}
	@Override
	public void onChannelEnded(ServerPlayer caster, boolean interrupted) {
		if (interrupted) DomainManager.remove(caster);
	}
	// 2026-09-22 反馈修正：打断返还 20% CD（走统一规则，与其它法术一致），不再是 0 CD。
	@Override
	public int getInterruptedCooldown(int cooldown) {
		return github.com.gengyoubo.sscfa.spell.SpellCastingRules.interruptedCooldown(cooldown);
	}
	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) { DomainManager.activate(caster); }
}
