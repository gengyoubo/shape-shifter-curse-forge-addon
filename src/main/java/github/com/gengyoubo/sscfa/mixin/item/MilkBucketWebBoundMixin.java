package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.init.AddonEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Milk keeps the addon effects that have their own release conditions. */
@Mixin(MilkBucketItem.class)
public abstract class MilkBucketWebBoundMixin {
    @Redirect(method = "finishUsingItem", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/entity/LivingEntity;curePotionEffects(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean ssca$keepWebBoundAndSheepForm(LivingEntity entity, ItemStack milk) {
        MobEffectInstance web = entity.getEffect(AddonEffects.SPIDER_WEB_BOUND.get());
        MobEffectInstance sheep = entity.getEffect(AddonEffects.SHEEP_FORM.get());
        boolean cured = entity.curePotionEffects(milk);
        if (web != null && !entity.hasEffect(web.getEffect())) entity.addEffect(new MobEffectInstance(web));
        if (sheep != null && !entity.hasEffect(sheep.getEffect())) entity.addEffect(new MobEffectInstance(sheep));
        return cured;
    }
}
