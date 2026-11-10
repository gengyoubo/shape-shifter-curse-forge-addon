package github.com.gengyoubo.sscfa.neomagic.client;

import net.minecraft.nbt.CompoundTag;

/** NeoMagic 瀹㈡埛绔姸鎬佸崰浣嶅疄鐜帮細寰?NeoMagic 瀛愮郴缁熺Щ妞嶅悗鏇挎崲銆?*/
public final class NeoClient {
    public static final CompoundTag hud = new CompoundTag();

    private NeoClient() {}

    public static void renderPixelBarBeforeChat(net.minecraft.client.gui.GuiGraphics context, float tickDelta) {}
}
