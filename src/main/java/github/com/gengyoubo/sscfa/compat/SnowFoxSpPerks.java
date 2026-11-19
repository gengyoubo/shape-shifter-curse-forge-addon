package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * 雪狐 SP / 霜脊的 perk 效果。轻踏（elusive paws）把方块触及距离乘以 1.5：Fabric 版缩放 Reach Entity
 * Attributes 的最终结果；Forge 端用 {@code forge:block_reach} 的 MULTIPLY_TOTAL 修饰符实现，服务端每 tick
 * 对账，属性随原版同步到客户端。能力由 {@link AddonPerkTrees} 的雪狐树授予。
 */
public final class SnowFoxSpPerks {
    public static final ResourceLocation FROST_DIVE = addon("perk_snow_fox_sp_frost_dive");
    public static final ResourceLocation ELUSIVE_PAWS = addon("perk_snow_fox_sp_elusive_paws");
    private static final UUID REACH = UUID.fromString("6f1c3d1e-6b0a-4e4b-9a52-6a1f7c1d2e91");
    private static boolean registered;

    private SnowFoxSpPerks() {}

    public static boolean isSupported(Player player) {
        return FormUtils.isAnyForm(player, FormIdentifiers.SNOW_FOX_SP, FormIdentifiers.SNOW_FOX_FROSTSPINE);
    }

    public static boolean hasElusivePaws(Player player) {
        return player != null && isSupported(player) && ResourceBars.hasPowerId(player, ELUSIVE_PAWS);
    }

    public static void registerEvents() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) updateReach(event.player);
        });
    }

    public static void updateReach(Player player) {
        var reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reach == null) return;
        boolean wanted = player.isAlive() && hasElusivePaws(player);
        if (wanted == (reach.getModifier(REACH) != null)) return;
        if (wanted) {
            reach.addTransientModifier(new AttributeModifier(REACH, "SSCA elusive paws reach", 0.5,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else {
            reach.removeModifier(REACH);
        }
    }

    private static ResourceLocation addon(String path) {
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
