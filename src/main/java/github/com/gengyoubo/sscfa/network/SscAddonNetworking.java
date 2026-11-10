package github.com.gengyoubo.sscfa.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柛婵勫劗閸嬫挸顫濋悡搴☆潾闂侀€炲苯澧柛鎴濈秺瀹曟粓鎮㈤悡搴ｇ暰闂佸壊鍋呴幆濠囨偄閸忕厧娈愰梺鍐叉惈閸犳稓妲愰幘顔解拻濞达絿鍎ら崵鈧銈嗘处閸樹粙骞堥妸鈺傚€婚柦妯侯槺椤斿姊洪崫鍕偍闁搞劍妞介崺娑㈠箣閻愵亙绨婚梺瑙勬緲婢у酣宕氭导瀛樼厱婵犻潧妫欓幆鍫ユ?ID 闂傚倸鍊搁崐鎼佸磹閹间礁纾圭紒瀣嚦濞戞鏃堝焵椤掑啰浜辨繝寰锋澘鈧洟骞婃惔锝囦笉闁哄稁鍘肩粻瑙勭箾閿濆骸澧┑鈥茬矙閺岋繝宕担绋库拫闂佸搫鏈惄顖炲极閸屾稒濮滈柟绋垮濞戭湱e 闂傚倸鍊搁崐鐑芥嚄閸撲礁鍨濇い鏍亼閳ь剙鍟村畷銊р偓娑櫭禍閬嶆⒑缂佹ê濮囬柣蹇斿哺閿濈偤寮撮悢绋垮伎濠碘槅鍨辩€笛呮兜閻愵剚浜ゆ繝闈涙川缁犻箖鏌熼幆褏鎽犻柣銊﹀灴閺岋絽螖娴ｇ鑳秗ic 闂傚倸鍊搁崐鐑芥嚄閸撲礁鍨濇い鏍亼閳ь剙鍟村畷銊р偓娑櫭禍閬嶆椤愩垺澶勭紒瀣灴瀵?{@code ServerPlayNetworking} ?ResourceLocation
 * 鍒嗛€氶亾锛涜繖閲屼繚鐣欏悓鏍风殑閫?????ID锛屽疄闄呮敹鍙??{@link AddonNetwork}? *
 * <p>鐩墠鍙０鏄庢硶鏈郴缁熼渶瑕佺殑閫氶亾锛涘叾浣欒兘鍔涚浉鍏抽€氶亾闅忚兘鍔涘眰绉绘閫愭琛?????/p>
 */
public final class SscAddonNetworking {
	public static final ResourceLocation PACKET_SPELL_CAST = new ResourceLocation("my_addon", "spell_cast");
	public static final ResourceLocation PACKET_SPELL_SELECT = new ResourceLocation("my_addon", "spell_select");
	public static final ResourceLocation PACKET_SPELL_SET_CAST_LEVEL = new ResourceLocation("my_addon", "spell_set_cast_level");
	public static final ResourceLocation PACKET_SPELL_CAST_DOWNGRADED = new ResourceLocation("my_addon", "spell_cast_downgraded");
	public static final ResourceLocation PACKET_SPELL_CONFIG_SYNC = new ResourceLocation("my_addon", "spell_config_sync");
	public static final ResourceLocation PACKET_TIDAL_TETHER = new ResourceLocation("my_addon", "tidal_tether");

	public static final ResourceLocation PACKET_EVO_SELECT_ROUTE = new ResourceLocation("my_addon", "evo_select_route");
	public static final ResourceLocation PACKET_EVO_SELECT_BRANCH = new ResourceLocation("my_addon", "evo_select_branch");
	public static final ResourceLocation PACKET_EVO_UNLOCK = new ResourceLocation("my_addon", "evo_unlock");
	public static final ResourceLocation PACKET_EVO_UNLOCK_BATCH = new ResourceLocation("my_addon", "evo_unlock_batch");
	public static final ResourceLocation PACKET_SSCA_START_ROUTE = new ResourceLocation("my_addon", "ssca_start_route");
	public static final ResourceLocation PACKET_JOB_CHANGE_CONFIRM = new ResourceLocation("my_addon", "job_change_confirm");
	public static final ResourceLocation PACKET_EVO_OPEN = new ResourceLocation("my_addon", "evo_open");

	private SscAddonNetworking() {}

	/** 鏈嶅姟绔帴鏀跺櫒娉ㄥ唽鍏ュ彛锛???{@link AddonNetwork#init()} 涔嬪悗璋???*/
	public static void registerServerReceivers() {
		AddonNetwork.registerServerReceiver(PACKET_SPELL_CAST, (player, buf) -> {
			int slot = buf.readVarInt();
			int token = buf.readVarInt();
			github.com.gengyoubo.sscfa.spell.SpellCastManager.cast(player, slot, token);
		});
		AddonNetwork.registerServerReceiver(PACKET_SPELL_SELECT, (player, buf) -> {
			int slot = buf.readVarInt();
			github.com.gengyoubo.sscfa.spell.SpellCastManager.setSelected(player, slot);
		});
		AddonNetwork.registerServerReceiver(PACKET_SPELL_SET_CAST_LEVEL, (player, buf) -> {
			int slot = buf.readVarInt();
			int castLevel = buf.readVarInt();
			github.com.gengyoubo.sscfa.spell.SpellCastManager.setCastLevel(player, slot, castLevel);
		});
		AddonNetwork.registerServerReceiver(PACKET_SPELL_CAST_DOWNGRADED, (player, buf) -> {
			int slot = buf.readVarInt();
			int token = buf.readVarInt();
			github.com.gengyoubo.sscfa.spell.SpellCastManager.castDowngraded(player, slot, token);
		});
		AddonNetwork.registerServerReceiver(PACKET_EVO_SELECT_ROUTE, (player, buf) ->
				github.com.gengyoubo.sscfa.evolution.EvolutionManager.selectRoute(player, buf.readUtf(256)));
		AddonNetwork.registerServerReceiver(PACKET_EVO_SELECT_BRANCH, (player, buf) ->
				github.com.gengyoubo.sscfa.evolution.EvolutionManager.selectBranch(player, buf.readUtf(256)));
		AddonNetwork.registerServerReceiver(PACKET_EVO_UNLOCK, (player, buf) ->
				github.com.gengyoubo.sscfa.evolution.EvolutionManager.tryUnlock(player, buf.readUtf(256)));
		AddonNetwork.registerServerReceiver(PACKET_EVO_UNLOCK_BATCH, (player, buf) -> {
			int count = Math.max(0, Math.min(64, buf.readInt()));
			java.util.List<String> ids = new java.util.ArrayList<>(count);
			for (int i = 0; i < count; i++) ids.add(buf.readUtf(256));
			github.com.gengyoubo.sscfa.evolution.EvolutionManager.tryUnlockBatch(player, ids);
		});
		AddonNetwork.registerServerReceiver(PACKET_SSCA_START_ROUTE, (player, buf) ->
				github.com.gengyoubo.sscfa.evolution.EvolutionManager.startSscaRoute(player, buf.readUtf(256)));
		AddonNetwork.registerServerReceiver(PACKET_JOB_CHANGE_CONFIRM, (player, buf) ->
				github.com.gengyoubo.sscfa.evolution.EvolutionManager.startJobChange(player, buf.readUtf(256)));
		AddonNetwork.registerServerReceiver(PACKET_EVO_OPEN, (player, buf) ->
				github.com.gengyoubo.sscfa.evolution.EvolutionManager.checkAndMigrate(player));
	}

	/** 鐧??GUI 鍚屾锛堣兘鍔涘眰绉绘鍚庢帴鍏?????*/
	public static void syncFallenAllayVexWindow(ServerPlayer player, int windowTicks) {}
	public static void syncFrostSpikeChargeState(ServerPlayer player, boolean charging) {}

	public static void sendWhitelistSync(ServerPlayer player) {
		// no-op until the whitelist GUI layer is ported
	}

	public static void sendWebHighlight(ServerPlayer player, int entityId, int ticks, int color) {}
	public static void sendDreamVeil(ServerPlayer player, int ticks, java.util.UUID nightmareId) {}
	public static void sendFearState(ServerPlayer player, int ticks) {}
	public static void sendFearHide(ServerPlayer player, java.util.UUID nightmareId, int ticks) {}
	public static void sendFearReveal(ServerPlayer player, java.util.UUID nightmareId, int ticks) {}
	public static void syncNovaLeapWindow(ServerPlayer player, int ticks) {}
	public static void sendSpookGhost(ServerPlayer player, java.util.UUID id, int ticks) {}
	public static void syncClawState(ServerPlayer player, int phase, float progress) {}
	public static void syncClawState(ServerPlayer player, int phase, float progress, float step) {}
	public static void syncJumpKillSilk(ServerPlayer player, boolean active) {}
	public static void syncDashState(ServerPlayer player, int ticks, float progress) {}
	public static void syncJumpKillSilk(ServerPlayer player, boolean active, double x, double y, double z) {}
	public static void syncJumpKillSilk(ServerPlayer player, boolean active, double x, double y, double z, int ticks) {}
	public static void syncDashState(ServerPlayer player, int phase, double targetY, double maxRange, double landingRadius) {}
	public static void syncSwingState(ServerPlayer player, int phase, double x, double y, double z) {}
	public static void syncSwingState(ServerPlayer player, boolean active, double x, double y, double z) {}
	public static void syncSwingState(ServerPlayer player, boolean active, double x, double y, double z, double ropeLen, int state, boolean canExtend, int tetherId) {}
	public static void syncSpearChargeState(ServerPlayer player, boolean charging) {}
	public static void sendOpenJobChange(ServerPlayer player) {}
}
