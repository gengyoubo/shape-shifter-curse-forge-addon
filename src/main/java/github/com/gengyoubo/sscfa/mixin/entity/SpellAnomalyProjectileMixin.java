package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext;
import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A projectile keeps the launch spell even after its caster switches scrolls or logs out. */
@Mixin(Projectile.class)
public abstract class SpellAnomalyProjectileMixin implements SpellAnomalyHits.ProjectileData {
    @Unique private SpellAnomalyContext ssca$anomaly;

    @Override public SpellAnomalyContext ssca$anomaly() { return ssca$anomaly; }
    @Override public void ssca$anomaly(SpellAnomalyContext context) { ssca$anomaly = context; }

    @Inject(method = "setOwner", at = @At("TAIL"))
    private void ssca$bindAnomaly(Entity owner, CallbackInfo ci) {
        var context = SpellAnomalyHits.current();
        if (context != null && owner != null && context.casterUUID().equals(owner.getUUID()))
            ssca$anomaly = context;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ssca$writeAnomaly(CompoundTag nbt, CallbackInfo ci) {
        if (ssca$anomaly != null) nbt.put("SscaSpellAnomaly", ssca$anomaly.write());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ssca$readAnomaly(CompoundTag nbt, CallbackInfo ci) {
        ssca$anomaly = nbt.contains("SscaSpellAnomaly", Tag.TAG_COMPOUND)
                ? SpellAnomalyContext.read(nbt.getCompound("SscaSpellAnomaly")) : null;
    }
}
