package github.com.gengyoubo.sscfa.spell.research;

import net.minecraft.network.chat.Component;

/** 濡叉垝缍呴惍鏃傗敀缁狅紕鎮婇崳銊ュ窗娴ｅ秲鈧?*/
public final class SlottedResearchManager {
    public static final int OPEN = 20, SAVE = 21, TEST = 22, COMPLETE = 23, TRAIN = 24, INSPECT = 25;

    private SlottedResearchManager() {}

    public static github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity context(net.minecraft.server.level.ServerPlayer player, int syncId) { return null; }

    public static void handle(net.minecraft.server.level.ServerPlayer player, int syncId, net.minecraft.nbt.CompoundTag request) {}

    public static Component text(String key, Object... args) {
        return Component.translatable("text.ssc_addon.research." + key, args);
    }
}
