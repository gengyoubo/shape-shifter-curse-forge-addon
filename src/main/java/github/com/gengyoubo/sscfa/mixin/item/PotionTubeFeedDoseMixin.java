package github.com.gengyoubo.sscfa.mixin.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import github.com.gengyoubo.sscfa.item.PotionTubeDose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.onixary.shapeShifterCurseForge.effect.FeedEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Forge 闂佹彃绉撮崯鎾绘晬濮橆偆鐭岄柛锔哄妼鐢洨绱撻埡鍕幋闂佹彃绻楅惁顖滅不閳ヨ尙鐟愬☉鎾愁儐閺嬪啴宕橀崨鎵闁轰焦娼欓弶鐑橆槹閻斿嘲顣撮梺鎻掗獜缁辨瑩鎯勯鐣屽灱闁衡偓闁稖绀?Forge FeedEffect闁挎稑顦埀?*/
@Mixin(value = FeedEffect.class, remap = false)
public abstract class PotionTubeFeedDoseMixin {
    @ModifyExpressionValue(method = "applyInstantenousEffect", remap = false,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D", remap = false))
    private double ssca$tubeFeedFraction(double original, @Local(argsOnly = true) LivingEntity target) {
        return PotionTubeDose.feedFraction(target, original);
    }

    @WrapOperation(method = "applyInstantenousEffect", remap = false,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;ceil(D)D", remap = false))
    private double ssca$tubeFeedFood(double amount, Operation<Double> original,
                                    @Local(argsOnly = true) LivingEntity target) {
        PotionTubeDose.captureFeedFood(target, amount);
        return original.call(amount);
    }

    @WrapOperation(method = "applyInstantenousEffect", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void ssca$tubeFeedSaturation(FoodData hunger, int food, float saturation,
                                         Operation<Void> original, @Local(argsOnly = true) LivingEntity target) {
        original.call(hunger, food, PotionTubeDose.feedSaturation(target, food, saturation));
    }
}
