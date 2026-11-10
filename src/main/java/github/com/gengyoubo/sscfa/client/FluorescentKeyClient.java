package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 鑽у厜骞肩伒鎸夐敭妫€娴嬪櫒锛堝鎴风杈规部瑙﹀彂锛夈€?
 * 涓昏閿?sp_primary)锛氳崸鍏夊辜鐏靛舰鎬佷笅鎸変笅 鈫?鍙戞硶闃垫縺鍏夊寘銆?
 * 娆¤閿?sp_secondary)锛氳崸鍏夊辜鐏靛舰鎬佷笅鎸変笅 鈫?鍙戞疆姹愭尝鍔ㄥ寘銆?
 * 閲囩敤杈规部瑙﹀彂锛坧ressed && !wasPressed锛夛紝閬垮厤闀挎寜杩炲彂銆?
 */
public final class FluorescentKeyClient {
	private static boolean wasPrimary = false;
	private static boolean wasSecondary = false;

	private FluorescentKeyClient() {
	}

	public static void register() {
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> { if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) FluorescentKeyClient.onClientTick(Minecraft.getInstance()); });
	}

	private static void onClientTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			wasPrimary = wasSecondary = false;
			return;
		}
		// 浠呰崸鍏夊辜鐏靛舰鎬佸搷搴?
		if (!FormUtils.isAxolotlFluorescent(player)) {
			wasPrimary = wasSecondary = false;
			return;
		}
		KeyMapping primary = SscAddonKeybindings.getPrimaryKey();
		KeyMapping secondary = SscAddonKeybindings.getSecondaryKey();

		boolean pNow = primary != null && primary.isDown();
		if (pNow && !wasPrimary) {
			SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FLUO_LASER);
		}
		wasPrimary = pNow;

		boolean sNow = secondary != null && secondary.isDown();
		if (sNow && !wasSecondary) {
			SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FLUO_TIDAL);
		}
		wasSecondary = sNow;
	}

}