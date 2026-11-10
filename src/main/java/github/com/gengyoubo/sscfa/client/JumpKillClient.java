package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 璺宠洓銆岃烦鏉€銆? 瀹㈡埛绔寜閿娴嬪櫒銆?
 * 浠呰烦铔涘舰鎬佺敓鏁堬紝鍗曚富閿紙sp_primary锛夎竟娌挎娴嬶細鎸変笅娌?鈫?寮€濮嬭搫鍔涳紱鏉惧紑娌?鈫?璺虫潃銆?
 * 鎵€鏈夊垽瀹氫笌缁撶畻鍦ㄦ湇鍔＄锛坽@code JumpKillManager}锛夈€?
 */
public final class JumpKillClient {

	private static boolean wasPressed = false;

	private JumpKillClient() {}

	public static void register() {
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> { if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) JumpKillClient.onClientTick(Minecraft.getInstance()); });
	}

	private static void onClientTick(Minecraft client) {
		LocalPlayer player = client.player;
		KeyMapping key = SscAddonKeybindings.getPrimaryKey();
		if (player == null || client.level == null || key == null
				|| !FormUtils.isForm(player, FormIdentifiers.SPIDER_SALTICIDAE)) {
			wasPressed = false;
			return;
		}
		boolean pressed = key.isDown();
		if (pressed && !wasPressed) {
			SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_JUMP_KILL_CHARGE_START);
		} else if (!pressed && wasPressed) {
			SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_JUMP_KILL_CHARGE_RELEASE);
		}
		wasPressed = pressed;
	}

}
