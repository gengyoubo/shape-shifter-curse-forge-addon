package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.item.PotionTubeDose;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.onixary.shapeShifterCurseForge.effect.FeedEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Forge 闂佹彃绉撮崯鎾绘晬濮橆偆鐭岄柛锔哄妼鐢洨绱撻埡鍕幋闂佹彃绻楅惁顖滅不閳ヨ尙鐟愬☉鎾愁儐閺嬪啴宕橀崨鎵闁轰焦娼欓弶鐑橆槹閻斿嘲顣撮梺鎻掗獜缁辨瑩鎯勯鐣屽灱闁衡偓闁稖绀?Forge FeedEffect闁挎稑顦埀?*/
@Mixin(value = FeedEffect.class, remap = false)
public abstract class PotionTubeFeedDoseMixin {
    @Redirect(method = {"applyInstantenousEffect", "m_19461_"}, remap = false,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D", remap = false))
    private double ssca$tubeFeedFraction(double minimum, double actual, Entity source, Entity attacker,
                                         LivingEntity target, int amplifier, double potency) {
        return PotionTubeDose.feedFraction(target, Math.max(minimum, actual));
    }

    @Redirect(method = {"applyInstantenousEffect", "m_19461_"}, remap = false,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;ceil(D)D", remap = false))
    private double ssca$tubeFeedFood(double amount, Entity source, Entity attacker,
                                    LivingEntity target, int amplifier, double potency) {
        PotionTubeDose.captureFeedFood(target, amount);
        return Math.ceil(amount);
    }

    @Redirect(method = {"applyInstantenousEffect", "m_19461_"}, remap = false,
            at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void ssca$tubeFeedSaturation(FoodData hunger, int food, float saturation,
                                         Entity source, Entity attacker, LivingEntity target, int amplifier, double potency) {
        hunger.eat(food, PotionTubeDose.feedSaturation(target, food, saturation));
    }
}
