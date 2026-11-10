package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.ability.VortexChargeManager;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * SP 缇庤タ铻堟缉娑¤搫鍔?- 瀹㈡埛绔寜閿娴嬪櫒銆?
 * 鎸?sp_primary锛氭湭钃勫姏 鈫?鍙戙€屽紑濮嬨€嶅寘锛涜搫鍔涗腑(vortex_state>0) 鈫?鍙戙€岄噴鏀俱€嶅寘銆?
 */
public final class VortexChargeClient {
	private static boolean wasKeyPressed = false;

	private VortexChargeClient() {
	}

	public static void register() {
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> { if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) VortexChargeClient.onClientTick(Minecraft.getInstance()); });
	}

	private static void onClientTick(Minecraft client) {
		LocalPlayer player = client.player;
		KeyMapping key = SscAddonKeybindings.getPrimaryKey();
		if (player == null || client.level == null || key == null) {
			wasKeyPressed = false;
			VortexChargeManager.setClientLocalCharging(false);
			return;
		}
		if (!FormUtils.isAxolotlSP(player)) {
			wasKeyPressed = false;
			VortexChargeManager.setClientLocalCharging(false);
			return;
		}
		// 姣?tick 缂撳瓨鏈湴鐜╁钃勫姏鏍囪锛屼緵纰版挒鎺ㄦ尋 mixin 蹇€熻鍙栵紙閬垮厤姣忔纰版挒璇?Apoli 璧勬簮锛?
		int vortexState = PowerUtils.getClientResourceValue(player, VortexChargeManager.VORTEX_STATE);
		VortexChargeManager.setClientLocalCharging(vortexState > 0);

		boolean pressed = key.isDown();
		if (pressed && !wasKeyPressed) {
			if (vortexState > 0) {
				SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_VORTEX_RELEASE);
			} else {
				SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_VORTEX_START);
			}
		}
		wasKeyPressed = pressed;
	}

}
