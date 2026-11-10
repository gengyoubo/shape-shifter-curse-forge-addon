package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.ability.DeathDomainRecovery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class DeathDomainRecoverySaveMixin {
    @Inject(method = "save", at = @At("TAIL"))
    private void ssca$confirmTerrainRecoverySave(ProgressListener progress, boolean flush, boolean savingDisabled,
                                                CallbackInfo ci) {
        if (flush && !savingDisabled) {
            ServerLevel world = (ServerLevel) (Object) this;
            DeathDomainRecovery.afterFlushedSave(world);
            github.com.gengyoubo.sscfa.spell.pocket.PocketSpaceManager.afterFlushedSave(world);
        }
    }
}
