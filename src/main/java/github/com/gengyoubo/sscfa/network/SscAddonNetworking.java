package github.com.gengyoubo.sscfa.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 闂傚倸鍊搁崐鎼佸磹閸濄儮鍋撳鐓庡闁逞屽墯閸戝綊宕滈悢鐓庣畾闁割偅鎯婇悢鍏煎殐闁冲搫鍠涚槐鎾⒒娴ｇ儤鍤€妞ゆ洦鍘介幈銊╂倻閽樺顔夐梺鍝勬储閸ㄦ椽鍩涢幋鐐簻闁规澘澧庨崚浼存煕濠靛棙鎯堥棁?ID 闂傚倸鍊搁崐鎼佸磹缁嬭娑樜旈埀顒冪亱濠德板€曢幊搴ｇ不閺嶎厼绠规繛锝庡墮婵′粙鏌￠崪浣稿⒋闁哄本鐩弫鍌涙姜閹稿孩娑e 闂傚倸鍊烽懗鍓佸垝椤栫偑鈧啴宕ㄧ€涙ê浜遍梺缁樺姇閻忔岸锝為弴鐔稿弿婵☆垱瀵х涵鐐交濠靛洨绠鹃柟鎯х摠閻ㄦ垿鏌ｅΔ浣稿胶ric 闂傚倸鍊烽懗鍓佸垝椤栫偑鈧啴宕ㄧ€涙ê浜遍棅顐㈡处缁嬫垿寮?{@code ServerPlayNetworking} ?ResourceLocation
 * 分通道；这里保留同样的通?????ID，实际收发??{@link AddonNetwork}? *
 * <p>目前只声明法术系统需要的通道；其余能力相关通道随能力层移植逐步补?????/p>
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

	/** 服务端接收器注册入口（???{@link AddonNetwork#init()} 之后调???*/
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

	/** 白??GUI 同步（能力层移植后接入?????*/
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
}
