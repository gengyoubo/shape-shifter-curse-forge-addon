package github.com.gengyoubo.sscfa.mixin.player;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.spell.FrostArmorAccess;
import github.com.gengyoubo.sscfa.spell.FrostArmorState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class FrostArmorAbsorptionMixin implements FrostArmorAccess {
    @Unique private FrostArmorState ssca$frostArmorState;

    @Override
    public FrostArmorState ssca$frostArmor() {
        if (ssca$frostArmorState == null) ssca$frostArmorState = new FrostArmorState();
        return ssca$frostArmorState;
    }

    @Inject(method = "setAbsorptionAmount", at = @At("HEAD"))
    private void ssca$consumeFrostArmor(float amount, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self instanceof github.com.gengyoubo.sscfa.ability.ParasiticAbsorptionAccess access)
            access.ssca$parasiticAbsorption().onChanged(self, self.getAbsorptionAmount(), Math.max(0, amount));
        if (ssca$frostArmorState != null) ssca$frostArmorState.onAbsorptionChanged(
                ((Player) (Object) this).getAbsorptionAmount(), Math.max(0, amount));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ssca$saveFrostArmor(CompoundTag nbt, CallbackInfo ci) {
        if (ssca$frostArmorState != null) ssca$frostArmorState.write(nbt);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ssca$loadFrostArmor(CompoundTag nbt, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        ssca$frostArmor().read(nbt, self, self.hasEffect(SscAddon.FROST_ARMOR));
        if (self instanceof github.com.gengyoubo.sscfa.ability.ParasiticAbsorptionAccess access)
            access.ssca$parasiticAbsorption().read(nbt, self);
    }
}
