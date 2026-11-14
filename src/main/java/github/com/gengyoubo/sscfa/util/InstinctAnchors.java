package github.com.gengyoubo.sscfa.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.effect.TransformativeStatusEffect;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.TransformManager;
import net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService;
import net.onixary.shapeShifterCurseForge.power.InstinctService;

/** 物品提供的独立形态锚定，不修改 SSC 自身的本能锁。 */
public final class InstinctAnchors {
    private InstinctAnchors() {}

    public enum Result { FIXED, RELEASED, CURSED_MOON, NATIVE_LOCKED, TRANSFORMING, ALREADY_FIXED, NOT_FIXED }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || !locked(player)) return;
            if (!changesForm(event.getItemStack())) return;
            player.displayClientMessage(Component.translatable("message.ssc_addon.instinct_anchor.blocked_item"), true);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        });
    }

    private static boolean changesForm(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && switch (id.toString()) {
            case "shape-shifter-curse:catalyst", "shape-shifter-curse:powerful_catalyst",
                    "shape-shifter-curse:inhibitor", "shape-shifter-curse:powerful_inhibitor",
                    "shape-shifter-curse:creative_inhibitor", "shape-shifter-curse:moondust_matrix",
                    "shape-shifter-curse:untreated_moondust", "ssc_addon:sp_upgrade_thing",
                    "ssc_addon:evolution_stone" -> true;
            default -> false;
        }) return true;
        return PotionUtils.getMobEffects(stack).stream().anyMatch(e -> e.getEffect() instanceof TransformativeStatusEffect);
    }

    public static boolean locked(Player player) {
        return player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null
                && InstinctAnchorSavedData.get(serverPlayer.getServer()).isLocked(player.getUUID());
    }

    public static boolean blocksChange(Player player, FormDefinition target) {
        if (!(player instanceof ServerPlayer) || !locked(player) || target == null) return false;
        FormDefinition current = FormManager.current(player);
        return current == null || !current.id().equals(target.id());
    }

    public static Result apply(ServerPlayer player, boolean fix) {
        var server = player.getServer();
        if (server == null) return Result.NATIVE_LOCKED;
        if (CursedMoonService.isInCursedMoon(server.overworld())) return Result.CURSED_MOON;
        if (TransformManager.isTransforming(player)) return Result.TRANSFORMING;
        var data = SscApi.currentForm(player).orElse(null);
        if (data == null) return Result.NATIVE_LOCKED;
        var anchors = InstinctAnchorSavedData.get(server);
        if (fix) {
            if (anchors.isLocked(player.getUUID())) return Result.ALREADY_FIXED;
            FormDefinition form = FormManager.current(player);
            if (data.isCursedMoonApplied() || form == null || form.hasFlag("no_instinct")
                    || form.hasFlag("lock_instinct") || TransformManager.isInstinctLocked(player)) return Result.NATIVE_LOCKED;
            anchors.setLocked(player.getUUID(), true);
            data.setInstinctRate(0);
        } else {
            if (!anchors.isLocked(player.getUUID())) return Result.NOT_FIXED;
            anchors.setLocked(player.getUUID(), false);
            CursedMoonService.applyEndCursedMoonEffect(player);
        }
        if (!(player instanceof net.minecraftforge.common.util.FakePlayer)) InstinctService.synchronizeHud(player);
        return fix ? Result.FIXED : Result.RELEASED;
    }
}
