package github.com.gengyoubo.sscfa.mixin;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/**
 * SSC Forge keeps JSON power cooldowns (per player, ticks left) in a private map and has no API to
 * clear one; the Netherjudge's undying totem must come off cooldown when its holder dies.
 */
@Mixin(value = FormActivePowerService.class, remap = false)
public interface FormActivePowerServiceAccessor {
    @Accessor(value = "COOLDOWNS", remap = false)
    static Map<UUID, Map<ResourceLocation, Integer>> ssca$cooldowns() {
        throw new AssertionError();
    }
}
