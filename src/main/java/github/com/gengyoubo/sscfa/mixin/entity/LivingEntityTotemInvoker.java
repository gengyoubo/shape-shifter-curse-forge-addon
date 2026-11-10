package github.com.gengyoubo.sscfa.mixin.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityTotemInvoker {
    @Invoker("checkTotemDeathProtection")
    boolean sscAddon$tryUseTotem(DamageSource source);
}
