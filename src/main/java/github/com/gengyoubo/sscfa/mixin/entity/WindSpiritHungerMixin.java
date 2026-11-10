package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.compat.WindSpiritHungerState;
import github.com.gengyoubo.sscfa.compat.WindSpiritPerks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import github.com.gengyoubo.sscfa.compat.NovaPerks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class WindSpiritHungerMixin implements WindSpiritHungerState {
    @Shadow private int foodLevel;
    @Unique private Player ssca$owner;
    /** Hundredths prepaid by rounding a previous direct food bill up. Survives reload/rank changes. */
    @Unique private int ssca$foodCredit;

    @Override public void ssca$setOwner(Player player) { ssca$owner = player; }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ssca$bindOwner(Player player, CallbackInfo ci) { ssca$owner = player; }

    @Unique private int ssca$discount() {
        if (ssca$owner == null || ssca$owner.level().isClientSide) return 0;
        int windDiscount = WindSpiritPerks.hungerDiscount(ssca$owner);
        return windDiscount != 0 ? windDiscount : NovaPerks.hungerDiscount(ssca$owner);
    }

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float ssca$reduceExhaustion(float amount) {
        return amount > 0 ? amount * (100 - ssca$discount()) / 100.0f : amount;
    }

    @Unique private int ssca$payFood(int cost) {
        int discount = ssca$discount();
        if (cost <= 0 || discount == 0) return cost;
        long bill = (long) cost * (100 - discount) - ssca$foodCredit;
        int paid = bill <= 0 ? 0 : (int) ((bill + 99) / 100);
        ssca$foodCredit = (int) ((long) paid * 100 - bill);
        return paid;
    }

    @ModifyVariable(method = "setFoodLevel", at = @At("HEAD"), argsOnly = true)
    private int ssca$reduceDirectFoodCost(int requestedLevel) {
        return requestedLevel < foodLevel ? foodLevel - ssca$payFood(foodLevel - requestedLevel) : requestedLevel;
    }

    @ModifyVariable(method = "eat", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int ssca$reduceNegativeFood(int food) { return food < 0 ? -ssca$payFood(-food) : food; }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ssca$saveCredit(CompoundTag nbt, CallbackInfo ci) { nbt.putInt("sscaWindFoodCredit", ssca$foodCredit); }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ssca$loadCredit(CompoundTag nbt, CallbackInfo ci) {
        ssca$foodCredit = Math.max(0, Math.min(99, nbt.getInt("sscaWindFoodCredit")));
    }
}
