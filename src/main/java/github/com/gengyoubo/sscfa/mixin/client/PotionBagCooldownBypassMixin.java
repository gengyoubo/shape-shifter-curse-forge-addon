package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.item.PotionBagItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Allow crouch-use to open the potion bag during its drinking cooldown. */
@Mixin(ServerPlayerGameMode.class)
public class PotionBagCooldownBypassMixin {

    @Redirect(
            method = "useItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemCooldowns;isOnCooldown(Lnet/minecraft/world/item/Item;)Z"),
            require = 0
    )
    private boolean ssc_addon$bypassCooldownForSneaking(ItemCooldowns manager, Item item,
            ServerPlayer player, Level level, ItemStack stack, InteractionHand hand) {
        return !(item instanceof PotionBagItem && player.isShiftKeyDown())
                && manager.isOnCooldown(item);
    }
}