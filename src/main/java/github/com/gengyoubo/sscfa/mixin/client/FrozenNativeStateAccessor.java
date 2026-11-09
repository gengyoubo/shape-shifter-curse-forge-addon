package github.com.gengyoubo.sscfa.mixin.client;

import net.minecraft.world.entity.AnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AnimationState.class)
public interface FrozenNativeStateAccessor {
    @Accessor("lastTime") void ssca$updatedAt(long value);
    @Accessor("accumulatedTime") void ssca$timeRunning(long value);
}
