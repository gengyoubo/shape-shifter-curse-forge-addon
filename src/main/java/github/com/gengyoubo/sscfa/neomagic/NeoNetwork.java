package github.com.gengyoubo.sscfa.neomagic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** NeoMagic 网络占位实现：待 NeoMagic 子系统移植后替换。 */
public final class NeoNetwork {
    public static final ResourceLocation HUD = new ResourceLocation("ssc_addon", "neomagic_hud");
    public static final ResourceLocation HUD_V2 = new ResourceLocation("ssc_addon", "neomagic_hud_v2");
    public static final ResourceLocation HUD_RESYNC_V2 = new ResourceLocation("ssc_addon", "neomagic_hud_resync_v2");

    private NeoNetwork() {}

    public static CompoundTag hudState(ServerPlayer player) {
        return new CompoundTag();
    }

    public static void close(ServerPlayer player) {
    }

    public static void clear() {
    }

    public static void hud(ServerPlayer player) {
    }

    public static void register() {
    }
}
