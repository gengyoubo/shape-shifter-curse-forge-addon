package github.com.gengyoubo.sscfa.neomagic.client;

import github.com.gengyoubo.sscfa.neomagic.NeoNetwork;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

/** 新魔法客户端镜像；服务端发送完整 HUD NBT，不做本地模拟。 */
public final class NeoClient {
    public static final CompoundTag hud = new CompoundTag();
    private static boolean registered;

    private NeoClient() {}

    public static void register() {
        if (registered) return;
        registered = true;
        AddonNetwork.registerClientReceiver(NeoNetwork.HUD, buffer -> update(buffer.readNbt()));
        AddonNetwork.registerClientReceiver(NeoNetwork.HUD_V2, buffer -> update(buffer.readNbt()));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> requestResync());
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> clear());
    }

    private static void update(CompoundTag state) {
        clear();
        if (state != null) hud.merge(state);
    }

    private static void clear() {
        for (String key : hud.getAllKeys().toArray(String[]::new)) hud.remove(key);
    }

    private static void requestResync() {
        AddonNetwork.sendToServer(NeoNetwork.HUD_RESYNC_V2, AddonNetwork.buffer());
    }

    /** 在聊天栏前绘制一条简洁的稳定性条；没有 HUD 数据时不占用屏幕空间。 */
    public static void renderPixelBarBeforeChat(GuiGraphics context, float tickDelta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !hud.getBoolean("Visible")) return;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        double current = Math.max(0, hud.getDouble("C"));
        double safety = Math.max(1, hud.getDouble("S"));
        double danger = Math.max(safety, hud.getDouble("DangerMax"));
        double quote = Math.max(0, hud.getDouble("Q"));
        boolean casting = hud.getInt("Age") >= 0;

        int x = 6;
        int y = screenHeight - 60;
        int width = 102;
        int height = 6;
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x90000000);
        context.fill(x, y, x + width, y + height, 0xFF252A2E);
        int safeWidth = (int) Math.round(width * Math.min(1, safety / danger));
        context.fill(x, y, x + safeWidth, y + height, 0xFF235A40);
        int currentWidth = (int) Math.round(width * Math.min(1, current / danger));
        int color = current > safety ? 0xFFB53A3A : 0xFF609637;
        context.fill(x, y, x + currentWidth, y + height, color);
        if (casting && quote > 0) {
            int projectedWidth = (int) Math.round(width * Math.min(1, (current + quote) / danger));
            context.fill(x + currentWidth, y, x + projectedWidth, y + height,
                    current + quote > safety ? 0x88F07D75 : 0x887CD4E5);
        }
        int marker = x + (int) Math.round(width * Math.min(1, safety / danger));
        context.fill(marker, y - 1, marker + 1, y + height + 1, 0xFFE9F5F2);
        context.drawString(minecraft.font,
                Component.literal(Math.round(current) + "/" + Math.round(safety)),
                x, y - 11, current > safety ? 0xFFF29D81 : 0xFFE5E9E7, false);
    }
}
