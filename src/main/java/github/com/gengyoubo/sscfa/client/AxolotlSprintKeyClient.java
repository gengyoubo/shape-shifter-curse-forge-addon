package github.com.gengyoubo.sscfa.client;

import io.netty.buffer.Unpooled;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 鏉╂稑瀵茬紘搴ゃ偪閾诲牊鎸夊ù浣稿暱閸?- 鐎广垺鍩涚粩顖樷偓宀€婀″锝囨煣鐠烘垿鏁妴宥勭瑐閹躲儱娅掗妴?
 * 娴犲懓绻橀崠鏍法鐟楄儻鐎宠ぐ銏♀偓浣风瑓閿涘本濡搁悧鈺冩倞閻ゆ崘绐囬柨顕嗙礄{@code options.sprintKey}閿涘娈戦幐澶夌秶閻樿埖鈧礁褰夐崠鏍︾瑐閹躲儲婀囬崝锛勵伂閿?
 * 娴ｆ寧婀囬崝锛勵伂閸栧搫鍨庨妴宀€婀″锝嗗瘻閻ゆ崘绐囬柨顔衡偓宥勭瑢閵嗗苯寮婚崙?W / 濞撳憡鍚甸懛顏勫З閻ゆ崘绐囬妴宥忕礉闁灝鍘ら崥搴濊⒈閼板懓顕ょ憴锕€褰傞崘鎻掑煛閵?
 */
public final class AxolotlSprintKeyClient {

	private static boolean lastSent = false;

	private AxolotlSprintKeyClient() {}

	public static void register() {
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> { if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) AxolotlSprintKeyClient.onClientTick(Minecraft.getInstance()); });
	}

	private static void onClientTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null || !FormUtils.isUpgradeAxolotl(player)) {
			if (lastSent) { // 缁傝绱戣ぐ銏♀偓渚婄窗鐞涖儱褰傛稉鈧▎?held=false閿涘矂妲婚張宥呭缁旑垱鐣悾?true
				send(false);
				lastSent = false;
			}
			return;
		}
		KeyMapping sprint = client.options.keySprint;
		boolean held = sprint != null && sprint.isDown();
		if (held != lastSent) {
			send(held);
			lastSent = held;
		}
	}

	private static void send(boolean held) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeBoolean(held);
		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToServer(SscAddonNetworking.PACKET_AXOLOTL_SPRINT_KEY, buf);
	}
}
