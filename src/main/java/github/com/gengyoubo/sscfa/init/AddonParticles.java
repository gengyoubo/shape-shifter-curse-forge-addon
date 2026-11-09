package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SscAddonForge.MOD_ID);

    /** 寒棘狐蓄力「汇聚冰晶」：匀速直线飞向中心、抵达即消失（客户端实现见 InwardIceParticle）。 */
    public static final RegistryObject<SimpleParticleType> INWARD_ICE = PARTICLES.register("inward_ice", () -> new SimpleParticleType(true));

    private AddonParticles() {}
}
