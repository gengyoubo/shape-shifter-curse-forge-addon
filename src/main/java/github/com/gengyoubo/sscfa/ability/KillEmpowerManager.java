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

	/** 鐠у鍏橀幐浣虹敾閺冨爼妫块敍?0s閿涘鈧?*/
	public static final int EMPOWER_TICKS = KillEmpowerState.WINDOW_TICKS;
	/** SP 鐠у鍏橀悘顐ゅ箚閸╄櫣顢呴弮鍫曟毐閿涘苯鐤勯梽鍛闂€澶镐簰閺傝棄濮為崥搴ｆ畱閻樿埖鈧焦鏅ラ弸婊€璐熼崙鍡幢姒涙顓?232閿涘矁绻嶇悰灞炬娴?balance 韫囶偆鍙庣拠璇插絿閿涘潊bilities.kill_empower_ring閿涘鈧?*/
	public static final int EMPOWER_RING_TICKS_SP = 232;
	/** Red 鐠у鍏橀悘顐ゅ箚閸╄櫣顢呴弮鍫曟毐閿涘苯鐤勯梽鍛闂€澶镐簰閺傝棄濮為崥搴ｆ畱閻樿埖鈧焦鏅ラ弸婊€璐熼崙鍡幢姒涙顓?280閿涘矁绻嶇悰灞炬娴?balance 韫囶偆鍙庣拠璇插絿閿涘潊bilities.kill_empower_ring閿涘鈧?*/
	public static final int EMPOWER_RING_TICKS_RED = 280;

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.kill_empower_ring");

	public static final int STATE_NONE = 0;
	public static final int STATE_READY = 1;   // 鐠у鍏樺鍛村櫞閺€鎾呯礄閸戠粯娼冪憴锕€褰傞敍?
	public static final int STATE_RING = 2;    // 鐠у鍏橀悘顐ゅ箚濠碘偓濞茶鑵?

	/** SP 鐠у鍏橀悘顐ゅ箚閺冨爼鏆遍敍鍧哸lance 韫囶偆鍙庨崣顖濐洬閻╂牭绱濋崶鐐衡偓鈧?EMPOWER_RING_TICKS_SP閿涘鈧?*/
	public static int ringTicksSp() {
		return BAL.i("ring_ticks_sp", EMPOWER_RING_TICKS_SP);
	}

	/** Red 鐠у鍏橀悘顐ゅ箚閺冨爼鏆遍敍鍧哸lance 韫囶偆鍙庨崣顖濐洬閻╂牭绱濋崶鐐衡偓鈧?EMPOWER_RING_TICKS_RED閿涘鈧?*/
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
		// ticks/ringTicks 閺勵垱鐦?tick 閹澘銈?-1 閻ㄥ嫬宕熺拫鍐ㄢ偓鎺曨吀閺冭绱檛ickFromRingEffect 闁?Math.max(0, x-1)閿涘绱?
		// 鐠х増澹掗柌蹇涱暕濞村鈧岸浜鹃敍娉倀ate 閺嶅洤绻?ring_duration 閺勵垳濮搁幀浣哥€锋禒缁樺壈鐠哄啿褰夐敍灞肩箽閹镐胶鐝涢崡鍐叉倱濮濄儻绱?026-09-24閿涘鈧?
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

	/** 閺勵垰鎯?SP娴ｅ潡鐡?/ 缁俱垹鐖奸拃鎴掑▏姒勬棑绱欑挧瀣厴鐞氼偄濮╅惃鍕⒈娑擃亜顔栨稉璇茶埌閹緤绱氶妴?*/
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
					playExtinguish(player);	// 閸掑洤鍤ぐ銏♀偓浣疯厬閺?
				}
				continue;
			}
			// 缁屾椽妫介幀浣风秵妫版垵鐣ч崡顐窗缁嬭櫕鈧緤绱檉lags==0閿涘褰ф禒?1 濞喡ょカ濠ф劘顕伴敍灞藉帳閹?readState 閸撯晙缍?2 鐠?+ writeState
			// 4 濞嗏€冲晸濮ｆ柨顕敍娑欑槨 100t 閸嬫矮绔村▎鈥崇暚閺佺顕伴崘娆掑殰閹板牞绱欐径鏍劥閸愭瑨鍓?EMPOWER_STATE 閺?5s 閸愬懏濯洪崶?0 閹緤绱氶妴?
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
					playExtinguish(player);	// 濮濊楠告稉顓熸焽
				}
				writeState(player, new KillEmpowerState(0, 0));
				continue;
			}
			var ringEffect = player.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
			if (state.hasRing() && (ringEffect == null
					|| player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get()))) {
				player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
				playExtinguish(player);	// 閺佸牊鐏夌悮顐㈩樆閸旀稒绔婚梽?/ 鐞氼偄鍣ｉ崠鏍﹁厬閺?
				state = state.stopRing();
			}
			KillEmpowerState next = state.tickFromRingEffect(ringEffect == null ? 0 : ringEffect.getDuration());
			if (next.hasRing() && next.ringTicks() % 16 == 0) {
				KillEmpowerCast.tickEmpowerRing(player);
			}
			if (state.hasRing() && !next.hasRing()) {
				player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
				playExtinguish(player);	// 閼奉亞鍔ч崚鐗堟埂缂佹挻娼?
			}
			writeState(player, next);
		}
	}

	/** 鐠у鍏橀悘顐ゅ箚缂佹挻娼?娑擃厽鏌囬惃鍕紑閻掓壆鍞查悘顓烇紣閿涘牅绗岄幍瀣З閸忓磭骞嗛妴浣诡劀鐢摜骞嗛崗宕囧箚闂婅櫕鏅ユ稉鈧懛杈剧礆閵?*/
	private static void playExtinguish(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
	}
}
