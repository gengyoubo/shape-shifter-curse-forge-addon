package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 璺宠洓銆屾瘨娑层€? 瀹㈡埛绔閿娴嬪櫒銆?
 * 浠呰烦铔涘舰鎬佺敓鏁堬紝娆￠敭锛坰p_secondary锛夋寜涓嬫部鍙戝寘锛涘熀纭€/涓濈嚎寮哄寲褰㈡€佸垽瀹氬叏鍦ㄦ湇鍔＄銆?
 */
public final class VenomSkillClient {

	private static boolean wasPressed = false;

	private VenomSkillClient() {}

	public static void register() {
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> { if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) VenomSkillClient.onClientTick(Minecraft.getInstance()); });
	}

	private static void onClientTick(Minecraft client) {
		LocalPlayer player = client.player;
		KeyMapping key = SscAddonKeybindings.getSecondaryKey();
		if (player == null || client.level == null || key == null
				|| !FormUtils.isForm(player, FormIdentifiers.SPIDER_SALTICIDAE)) {
			wasPressed = false;
			return;
		}
		boolean pressed = key.isDown();
		if (pressed && !wasPressed) {
			SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_VENOM_SKILL_PRESS);
		}
		wasPressed = pressed;
	}

}
