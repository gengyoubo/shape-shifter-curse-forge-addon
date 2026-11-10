package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.particle.ParticleOwnership;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/** 让原版粒子携带装饰性归属标记（主人 UUID / 弹道特效标记）。 */
@Mixin(Particle.class)
public abstract class ParticleOwnershipMixin implements ParticleOwnership {

    @Unique private UUID ssca$decorationOwner;
    @Unique private boolean ssca$projectileDecoration;

    @Override
    public UUID ssca$getDecorationOwner() {
        return ssca$decorationOwner;
    }

    @Override
    public void ssca$setDecorationOwner(UUID owner) {
        ssca$decorationOwner = owner;
    }

    @Override
    public boolean ssca$isProjectileDecoration() {
        return ssca$projectileDecoration;
    }

    @Override
    public void ssca$markProjectileDecoration() {
        ssca$projectileDecoration = true;
    }
}
