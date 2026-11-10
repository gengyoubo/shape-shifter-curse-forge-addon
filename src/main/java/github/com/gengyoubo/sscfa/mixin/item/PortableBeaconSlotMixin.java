package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.ability.AllaySPPortableBeacon;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class PortableBeaconSlotMixin {
    @Shadow @Final public Container container;

    @Inject(method = {"setByPlayer(Lnet/minecraft/world/item/ItemStack;)V", "set"}, at = @At("HEAD"))
    private void ssca$clearStoredBeacon(ItemStack stack, CallbackInfo ci) {
        if (AllaySPPortableBeacon.isActiveStack(stack)
                && (!(container instanceof Inventory playerInventory)
                || !stack.getTag().getUUID("ssca_beacon_owner").equals(playerInventory.player.getUUID()))) {
            AllaySPPortableBeacon.clearStack(stack);
        }
        // 濠电姷顣介崑鎾寸箾閼艰泛鍚规繛鍫熷灦缁嬪顓奸崱鈺佸Г闂佹悶鍎插畷姗€宕ュΟ灏栧亾濞戞顏堝矗閸℃稒顥堥柣鎰版涧鐠愮喖鎮楃憴鍕姸闁稿孩顨婂畷鐘诲川椤撗冩櫍闂侀潻缍嗛悡澶屾濞嗘垹涓嶉柛妤冨仧閹?闂佸憡鐟﹂崹鐢告儍閻樿闂柕濞у懏鎯ｉ梺鎸庣☉椤︻垰鐣甸崱妯虹窞闁告鍋涚粻鍨節绾版ê浜惧┑鐐跺紦缁鳖喚妲愬? 缂備礁鍊瑰Σ鎺楁閳哄倸绶為弶鍫涘妽濞呭繘鏌ㄥ☉姗嗘缂?        // 闁诲孩绋掗敋婵炲弶鐗犻幃鎶藉煛娓氬洤鏅欓梺鐓庮殠娴滄粎鎹㈡笟鈧幆鍐礋椤旂厧澹掗梺鍛婄墪閹冲繗娼曢梺鍛婂笚閻熴倗鎹㈠鑸靛仼婵炲棙鐟х粈鍑穕ot.inventory 婵?PlayerInventory 闂佸搫鍟﹢鍦箔婢跺鈧帡宕崘顏嗩槴
        if (github.com.gengyoubo.sscfa.ability.AllaySPTotem.isMarkedActiveTotem(stack)
                && !(container instanceof Inventory)) {
            github.com.gengyoubo.sscfa.ability.AllaySPTotem.clearTotemMark(stack);
        }
    }
}
