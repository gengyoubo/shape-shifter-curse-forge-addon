package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class KillEmpowerManager {

	private KillEmpowerManager() {}

	/** 赋能持续时间?0s）??*/
	public static final int EMPOWER_TICKS = KillEmpowerState.WINDOW_TICKS;
	/** SP 赋能火环基础时长，实际时长以施加后的状态效果为准；默??232，运行时??balance 快照读取（abilities.kill_empower_ring）??*/
	public static final int EMPOWER_RING_TICKS_SP = 232;
	/** Red 赋能火环基础时长，实际时长以施加后的状态效果为准；默??280，运行时??balance 快照读取（abilities.kill_empower_ring）??*/
	public static final int EMPOWER_RING_TICKS_RED = 280;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.kill_empower_ring");

	public static final int STATE_NONE = 0;
	public static final int STATE_READY = 1;   // 赋能待释放（击杀触发?
	public static final int STATE_RING = 2;    // 赋能火环激活??

	/** SP 赋能火环时长（balance 快照可覆盖，回??EMPOWER_RING_TICKS_SP）??*/
	public static int ringTicksSp() {
		return BAL.i("ring_ticks_sp", EMPOWER_RING_TICKS_SP);
	}

	/** Red 赋能火环时长（balance 快照可覆盖，回??EMPOWER_RING_TICKS_RED）??*/
	public static int ringTicksRed() {
		return BAL.i("ring_ticks_red", EMPOWER_RING_TICKS_RED);
	}

	private record BurnOrigin(UUID owner, int expiresAtAge, boolean normal) {}
	private static final Map<LivingEntity, BurnOrigin> BURNS = new WeakHashMap<>();
	private static final java.util.Set<UUID> ACTIVE_RINGS = new java.util.HashSet<>();

	public static void register() {
		github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(KillEmpowerManager::onTick);
		github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> {
			if (ACTIVE_RINGS.remove(handler.player.getUUID())) handler.player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
			if (isEmpowerForm(handler.player)) writeState(handler.player, new KillEmpowerState(0, 0));
			BURNS.values().removeIf(burn -> burn.owner().equals(handler.player.getUUID()));
		});
		github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(server -> {
			BURNS.clear();
			ACTIVE_RINGS.clear();
		});
	}

	public static KillEmpowerState readState(ServerPlayer player) {
		int flags = PowerUtils.getResourceValue(player, FormIdentifiers.EMPOWER_STATE);
		int ticks = PowerUtils.getResourceValue(player, FormIdentifiers.EMPOWER_TICKS);
		int ringTicks = PowerUtils.getResourceValue(player, FormIdentifiers.EMPOWER_RING_TICKS);
		return new KillEmpowerState((flags & STATE_READY) != 0 ? ticks : 0,
				(flags & STATE_RING) != 0 ? ringTicks : 0);
	}

	public static void writeState(ServerPlayer player, KillEmpowerState state) {
		if (state.hasRing()) ACTIVE_RINGS.add(player.getUUID());
		else ACTIVE_RINGS.remove(player.getUUID());
		// ticks/ringTicks 是??tick 恰??-1 的单调倒计时（tickFromRingEffect ??Math.max(0, x-1)）??
		// 走批量预测通道；state 标??ring_duration 是状态型任意跳变，保持立即同步??026-09-24）??
		PowerUtils.countDownAndSync(player, FormIdentifiers.EMPOWER_TICKS, state.readyTicks());
		PowerUtils.countDownAndSync(player, FormIdentifiers.EMPOWER_RING_TICKS, state.ringTicks());
		PowerUtils.setResourceValueAndSync(player, FormIdentifiers.EMPOWER_STATE, state.flags());
		if (!state.hasRing()) PowerUtils.setResourceValueAndSync(player, FormIdentifiers.EMPOWER_RING_DURATION, 0);
	}

	public static boolean damage(LivingEntity target, DamageSource source, float amount, boolean normalSkill) {
		boolean wasAlive = target.isAlive();
		boolean damaged = target.hurt(source, amount);
		if (normalSkill && wasAlive && damaged && !target.isAlive()
				&& source.getEntity() instanceof ServerPlayer player
				&& isEmpowerForm(player) && !WhitelistUtils.isProtected(player, target)) {
			writeState(player, readState(player).grant());
		}
		return damaged;
	}

	public static void trackBurn(ServerPlayer player, LivingEntity target, int duration, boolean normalSkill) {
		if (!target.isAlive()) return;
		BURNS.put(target, new BurnOrigin(player.getUUID(), target.tickCount + duration, normalSkill));
		if (target instanceof github.com.gengyoubo.sscfa.util.SscIgnitedEntityAccessor accessor) {
			accessor.sscAddon$setIgniterUuid(player.getUUID());
		}
	}

	public static boolean damageBurn(LivingEntity target, DamageSource source, float amount) {
		BurnOrigin burn = BURNS.get(target);
		boolean normalSkill = burn != null && burn.normal() && target.tickCount < burn.expiresAtAge()
				&& source.getEntity() != null && burn.owner().equals(source.getEntity().getUUID());
		return damage(target, source, amount, normalSkill);
	}

	/** 是??SP使??/ 红堕落使魔（赋能被动的两个宿主形态）?*/
	public static boolean isEmpowerForm(ServerPlayer player) {
		return FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_SP)
				|| FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);
	}

	private static void onTick(net.minecraft.server.MinecraftServer server) {
		BURNS.entrySet().removeIf(entry -> !entry.getKey().isAlive()
				|| entry.getKey().tickCount >= entry.getValue().expiresAtAge()
				|| !entry.getKey().hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.FOX_FIRE_BURN.get()));
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isEmpowerForm(player)) {
				if (ACTIVE_RINGS.remove(player.getUUID())) {
					player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
					playExtinguish(player);	// 切出形态中??
				}
				continue;
			}
			// 空闲态低频守卫：稳态（flags==0）只?1 次资源读，免??readState 剩??2 ??+ writeState
			// 4 次写比对；每 100t 做一次完整读写自愈（外部写??EMPOWER_STATE ??5s 内拉?0 态）?
			if (PowerUtils.getResourceValue(player, FormIdentifiers.EMPOWER_STATE) == STATE_NONE
					&& player.tickCount % 100 != 0) {
				continue;
			}
			KillEmpowerState state = readState(player);
			if (state.flags() == STATE_NONE) {
				writeState(player, state);
				continue;
			}
			if (!player.isAlive()) {
				if (state.hasRing()) {
					player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
					playExtinguish(player);	// 死亡中断
				}
				writeState(player, new KillEmpowerState(0, 0));
				continue;
			}
			var ringEffect = player.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
			if (state.hasRing() && (ringEffect == null
					|| player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get()))) {
				player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
				playExtinguish(player);	// 效果被外力清?/ 被净化中??
				state = state.stopRing();
			}
			KillEmpowerState next = state.tickFromRingEffect(ringEffect == null ? 0 : ringEffect.getDuration());
			if (next.hasRing() && next.ringTicks() % 16 == 0) {
				KillEmpowerCast.tickEmpowerRing(player);
			}
			if (state.hasRing() && !next.hasRing()) {
				player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
				playExtinguish(player);	// 自然到期结??
			}
			writeState(player, next);
		}
	}

	/** 赋能火环结??中断的火焰熄灭声（与手动关环、正常环关环音效一致）??*/
	private static void playExtinguish(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
	}
}
