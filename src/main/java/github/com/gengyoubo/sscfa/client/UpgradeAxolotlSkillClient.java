package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 杩涘寲缇庤タ铻堟妧鑳?- 瀹㈡埛绔寜閿娴嬪櫒銆?
 * 涓绘妧鑳介敭锛坰p_primary锛夆啋 鎶曟幏姘寸煕锛涙鎶€鑳介敭锛坰p_secondary锛夆啋 娑℃祦寮曞銆?
 * 浠呭湪杩涘寲缇庤タ铻堝舰鎬佷笅鍙戝寘锛涜妭鐐硅В閿?/ CD 鍒ゅ畾鍏ㄥ湪鏈嶅姟绔€?
 */
public final class UpgradeAxolotlSkillClient {
	private static boolean wasPrimaryPressed = false;
	private static boolean wasSecondaryPressed = false;

	private UpgradeAxolotlSkillClient() {
	}

	public static void register() {
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> { if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) UpgradeAxolotlSkillClient.onClientTick(Minecraft.getInstance()); });
	}

	private static void onClientTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null || !FormUtils.isUpgradeAxolotl(player)) {
			wasPrimaryPressed = false;
			wasSecondaryPressed = false;
			return;
		}
		KeyMapping primary = SscAddonKeybindings.getPrimaryKey();
		KeyMapping secondary = SscAddonKeybindings.getSecondaryKey();
		boolean p = primary != null && primary.isDown();
		boolean s = secondary != null && secondary.isDown();
		if (p && !wasPrimaryPressed) {
			SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_UPGRADE_AXOLOTL_SPEAR);
		}
		if (s && !wasSecondaryPressed) {
			SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_UPGRADE_AXOLOTL_VORTEX);
		}
		wasPrimaryPressed = p;
		wasSecondaryPressed = s;
	}

}
