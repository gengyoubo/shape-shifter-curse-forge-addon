package github.com.gengyoubo.sscfa.mixin.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.item.AllayJukeboxItem;
import github.com.gengyoubo.sscfa.item.PotionBagItem;
import github.com.gengyoubo.sscfa.item.PotionTestTubes;
import github.com.gengyoubo.sscfa.item.UniversalEnergyPotionItem;
import github.com.gengyoubo.sscfa.item.WitherPotionItem;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.RecordItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 版（依据上游源码重写）：药水袋/悦灵物品锁定与药水叠放上限。 */
@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerMixin {

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Shadow
    public abstract Slot getSlot(int index);

    @Shadow
    public abstract ItemStack getCarried();

    @Unique
    private Player ssc_addon$clickPlayer;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void ssc_addon$captureClickPlayer(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        this.ssc_addon$clickPlayer = player;
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void ssc_addon$clearClickPlayer(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        this.ssc_addon$clickPlayer = null;
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(int slotId, int mouseButton, ClickType type, Player player, CallbackInfo ci) {
        Slot slot = slotId >= 0 && slotId < this.slots.size() ? this.getSlot(slotId) : null;
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            if (stack.is(SscAddon.POTION_BAG)) {
                if (type == ClickType.PICKUP) {
                    ItemStack cursor = this.getCarried();
                    if (!cursor.isEmpty() && PotionBagItem.isStorable(cursor)
                            && PotionBagItem.insertIntoBag(stack, cursor, player) > 0) {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F,
                                0.8F + player.level().getRandom().nextFloat() * 0.4F);
                        slot.setChanged();
                    }
                }
                ci.cancel();
                return;
            }
            if (stack.is(SscAddon.ALLAY_HEAL_WAND)) {
                ci.cancel();
                return;
            }
            if (stack.is(SscAddon.ALLAY_JUKEBOX)) {
                ItemStack cursor = this.getCarried();
                if (!cursor.isEmpty() && cursor.getItem() instanceof RecordItem
                        && AllayJukeboxItem.tryChargeWithDisc(stack, cursor)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 1.0f, 1.5f);
                }
                ci.cancel();
            }
        } else if (type == ClickType.SWAP && mouseButton >= 0 && mouseButton < 9) {
            ItemStack hotbar = player.getInventory().getItem(mouseButton);
            if (hotbar.is(SscAddon.POTION_BAG) || hotbar.is(SscAddon.ALLAY_HEAL_WAND) || hotbar.is(SscAddon.ALLAY_JUKEBOX)) {
                ci.cancel();
            }
        }
    }

    @WrapOperation(method = "moveItemStackTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getMaxStackSize()I"),
            require = 0)
    private int ssc_addon$insertItemEmptySlotCap(Slot slot, Operation<Integer> original) {
        Player player = this.ssc_addon$clickPlayer;
        if (player != null && slot.container instanceof Inventory) {
            int limit = ssc_addon$potionInsertLimit(this.getCarried(), player);
            if (limit > 0) {
                return Math.min(limit, original.call(slot));
            }
        }
        return original.call(slot);
    }

    @Unique
    private int ssc_addon$potionInsertLimit(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) return -1;
        if (PotionTestTubes.hasCustomStackLimit(stack)) {
            return PotionTestTubes.stackLimit(stack, player);
        }
        if (stack.getItem() instanceof WitherPotionItem) {
            return WitherPotionItem.getStackLimitFor(player);
        }
        if (stack.getItem() instanceof UniversalEnergyPotionItem || stack.getItem() instanceof PotionItem) {
            return Math.max(1, stack.getMaxStackSize());
        }
        return -1;
    }
}
