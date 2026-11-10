package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.item.AddonAccessoryGuard;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge 闂佹彃绉撮崯鎾绘晬濮濆┘C 濠⒀呭仜瀹歌鲸顨滈弶鎸庢儌闁圭顦靛顔句沪閻愬灚鐣辩憸鑸灪閳ь兛绶氬娲礆閼哥數澧″Δ鐘茬焿缁辨瑩鎯勯鐣屽灱闁衡偓闁稖绀?Forge AccessoryItem闁挎稑顦埀?*/
@Mixin(value = AccessoryItem.class, remap = false)
public abstract class SscEnhancedAccessoryRestrictionMixin {
    @Inject(method = "canEquip", at = @At("HEAD"), cancellable = true, remap = false)
    private void ssca$checkEnhancedAccessory(ItemStack stack, LivingEntity entity,
                                             AccessoryItem.SlotData slotData,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!AddonAccessoryGuard.canEquipMappedAccessory(entity, (AccessoryItem) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
