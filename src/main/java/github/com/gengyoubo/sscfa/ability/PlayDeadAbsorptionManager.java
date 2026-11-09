package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 瑁呮鍥炶缁欎簣鐨勩€岄粍蹇冦€嶅瓨鐣?+ 琛板噺绠＄悊銆?
 * 瑙勫垯锛堢敤鎴?2026-06-23 纭锛夛細
 * - 浠呬綔鐢ㄤ簬銆岃姝荤粰鐨勯粍蹇冦€嶈繖閮ㄥ垎棰勭畻锛屽叾瀹冩潵婧愶紙濡傞噾鑻规灉锛夌殑榛勫績涓嶅姩銆?
 * - 璁℃椂浠庛€岃姝荤粨鏉熴€嶉偅涓€鍒昏捣绠楋紝瀛樼暀 30 绉掋€?
 * - 30 绉掑悗寮€濮嬭“鍑忥細姣忕鍑?1 棰楀績锛? 2 鐐瑰惛鏀?HP锛夛紝鐩存帴鐢?setAbsorptionAmount 鎵ｏ紝鏃犲彈鍑荤壒鏁堛€?
 */
public class PlayDeadAbsorptionManager {

	/** 瀵瑰鏆撮湶鍚?scope 璇诲彇鍣紙SscAddonActions 瑁呮 CD 鍏辩敤锛岄伩鍏嶅弻浠界淮鎶わ級銆?*/
	public static github.com.gengyoubo.sscfa.balance.BalanceReader balanceReader() {
		return EARLY_END_BAL;
	}

	private static final github.com.gengyoubo.sscfa.balance.BalanceReader EARLY_END_BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.playing_dead");
	// 銆岃姝婚粍蹇冦€嶉绠楋紙鍙湁杩欓儴鍒嗕細鍦?30s 鍚庤“鍑忥級
	private static final Map<UUID, Float> BUDGET = new ConcurrentHashMap<>();
	// 琛板噺寮€濮嬬殑鏈嶅姟绔?tick锛? 瑁呮缁撴潫閭ｅ埢 + 30s锛?
	private static final Map<UUID, Long> DECAY_START = new ConcurrentHashMap<>();

	private static final int RETAIN_TICKS = 600;      // 30 绉掑瓨鐣?
	private static final int DECAY_INTERVAL = 20;     // 姣忕琛板噺涓€娆?
	private static final float DECAY_PER_SEC = 2.0f;  // 姣忔鍑?1 棰楀績 = 2 HP

	// 闃舵 5锛氳繍琛屾椂蹇収璇诲彇锛坅bilities.playing_dead锛涘揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
	private static final BalanceReader BAL = new BalanceReader("abilities.playing_dead");

	private PlayDeadAbsorptionManager() {
	}

    private static final String SAVE_KEY = "ssca_play_dead_absorption";

    /** Preserve the owned budget across reconnects without keeping disconnected UUIDs in memory. */
    public static void writePlayerData(ServerPlayer player, net.minecraft.nbt.CompoundTag nbt) {
        float budget = Math.min(BUDGET.getOrDefault(player.getUUID(), 0f), player.getAbsorptionAmount());
        if (!Float.isFinite(budget) || budget <= 0) { nbt.remove(SAVE_KEY); return; }
        var data = new net.minecraft.nbt.CompoundTag();
        data.putFloat("Budget", budget);
        Long start = DECAY_START.get(player.getUUID());
        if (start != null) {
            long now = player.getServer().getTickCount();
            int interval = Math.max(1, BAL.i("decay_interval", DECAY_INTERVAL));
            long remaining = start > now ? start - now : Math.floorMod(start - now, interval);
            data.putLong("Remaining", remaining);
        }
        nbt.put(SAVE_KEY, data);
    }

    public static void readPlayerData(ServerPlayer player, net.minecraft.nbt.CompoundTag nbt) {
        forgetPlayer(player.getUUID());
        if (!nbt.contains(SAVE_KEY, 10)) return;
        var data = nbt.getCompound(SAVE_KEY);
        float budget = Math.min(data.getFloat("Budget"), player.getAbsorptionAmount());
        if (!Float.isFinite(budget) || budget <= 0) return;
        BUDGET.put(player.getUUID(), budget);
        if (data.contains("Remaining")) {
            long remaining = Math.max(0, Math.min(BAL.i("retain_ticks", RETAIN_TICKS), data.getLong("Remaining")));
            DECAY_START.put(player.getUUID(), player.getServer().getTickCount() + remaining);
        }
    }

    public static void forgetPlayer(UUID uuid) { BUDGET.remove(uuid); DECAY_START.remove(uuid); }
    public static void clearAll() { BUDGET.clear(); DECAY_START.clear(); }

	/** 瑁呮鏈熼棿姣忔缁欓粍蹇冩椂璋冪敤锛岀疮鍔犮€岃姝婚粍蹇冦€嶉绠楋紱骞舵妸琛板噺璁℃椂娓呴浂锛堢粨鏉熷悗鍐嶉噸鏂拌 30s锛夈€?*/
	public static void addAbsorption(Player player, float delta) {
		if (delta <= 0f) {
			return;
		}
		UUID id = player.getUUID();
		BUDGET.merge(id, delta, Float::sum);
		DECAY_START.remove(id);
	}

	public static void tick(ServerPlayer player) {
		UUID id = player.getUUID();
		Float budgetObj = BUDGET.get(id);
		if (budgetObj == null) {
			return;
		}

		// 棰勭畻涓嶈兘瓒呰繃瀹為檯榛勫績锛堣鎵撴帀/姝讳骸浼氬噺灏戝疄闄呴粍蹇冿級
		float actual = player.getAbsorptionAmount();
		float budget = Math.min(budgetObj, actual);
		if (budget <= 0f) {
			BUDGET.remove(id);
			DECAY_START.remove(id);
			return;
		}

		// 浠嶅湪瑁呮 鈫?杩樺湪绱Н锛屼笉琛板噺锛涜鏃惰捣鐐圭暀鍒扮粨鏉熷悗鍐嶈
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PLAYING_DEAD.get())) {
			BUDGET.put(id, budget);
			DECAY_START.remove(id);
			return;
		}

		// 瑁呮宸茬粨鏉?鈫?纭繚琛板噺璧风偣宸茶锛堢粨鏉熼偅鍒?+ 30s锛?
		long now = player.getServer().getTickCount();
		long decayStart = DECAY_START.computeIfAbsent(id, k -> now + BAL.i("retain_ticks", RETAIN_TICKS));

		// 鍒扮偣鍚庢瘡绉掑噺 2 HP锛堜唬鐮佺洿鎺ユ墸锛屾棤鍙楀嚮鐗规晥锛?
		if (now >= decayStart && (now - decayStart) % BAL.i("decay_interval", DECAY_INTERVAL) == 0) {
			float reduce = Math.min(budget, (float) BAL.d("decay_per_sec", DECAY_PER_SEC));
			github.com.gengyoubo.sscfa.spell.FrostArmorState.removeOther(player, reduce);
			budget -= reduce;
		}

		if (budget <= 0f) {
			BUDGET.remove(id);
			DECAY_START.remove(id);
		} else {
			BUDGET.put(id, budget);
		}
	}
}
