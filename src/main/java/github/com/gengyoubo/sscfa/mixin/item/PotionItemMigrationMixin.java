package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.item.PotionItemMigration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 闂侀潻璐熼崝宀€鏁锔藉亱闁割偆鍠庨惁濠氭煟濡炵粯娅呴柟纰辨箟D闂佸搫琚崕鏌ュ极閻愬搫绀冪€光偓閸愶腹鍋撻崘顔肩鐎广儱鐗婄粊顕€鏌熼獮鍨仾婵☆偒鍋婇幊锟狀敍濞戞瑥鐦籒BT闂佹寧绋戞總鏃傛崲濮樿埖鍋╂繛鍡楃箰閺傃囨煛閸繍妲兼い鏇氬嵆瀹曪綁寮借绾句粙鎮介婊庣劸闁归攱澹嗙划姘舵倻濡厧鐏叉繝銏ｆ硾鐎氼噣骞冩繝鍥?*/
@Mixin(ItemStack.class)
public abstract class PotionItemMigrationMixin {
    @ModifyVariable(method = "of(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static CompoundTag ssc_addon$migratePotionItem(CompoundTag source) {
        return PotionItemMigration.rewrite(source);
    }
}
