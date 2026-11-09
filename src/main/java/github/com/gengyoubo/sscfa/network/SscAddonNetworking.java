package github.com.gengyoubo.sscfa.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 附属网络通道 ID 集合（Forge 版）。Fabric 版用 {@code ServerPlayNetworking} 按 ResourceLocation
 * 分通道；这里保留同样的通道 ID，实际收发走 {@link AddonNetwork}。
 *
 * <p>目前只声明法术系统需要的通道；其余能力相关通道随能力层移植逐步补回。</p>
 */
public final class SscAddonNetworking {
	public static final ResourceLocation PACKET_SPELL_CAST = new ResourceLocation("my_addon", "spell_cast");
	public static final ResourceLocation PACKET_SPELL_SELECT = new ResourceLocation("my_addon", "spell_select");
	public static final ResourceLocation PACKET_SPELL_SET_CAST_LEVEL = new ResourceLocation("my_addon", "spell_set_cast_level");
	public static final ResourceLocation PACKET_SPELL_CAST_DOWNGRADED = new ResourceLocation("my_addon", "spell_cast_downgraded");
	public static final ResourceLocation PACKET_SPELL_CONFIG_SYNC = new ResourceLocation("my_addon", "spell_config_sync");
	public static final ResourceLocation PACKET_TIDAL_TETHER = new ResourceLocation("my_addon", "tidal_tether");

	private SscAddonNetworking() {}

	/** 服务端接收器注册入口（在 {@link AddonNetwork#init()} 之后调用）。 */
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
	}

	/** 白名单 GUI 同步（能力层移植后接入）。 */
	public static void sendWhitelistSync(ServerPlayer player) {
		// no-op until the whitelist GUI layer is ported
	}
}
