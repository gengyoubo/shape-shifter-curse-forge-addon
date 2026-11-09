package github.com.gengyoubo.sscfa.neomagic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 新魔法系统入口（命名空间 {@value #NAMESPACE}）。新魔法的注册、数据重载、网络和运行时都从这里挂到 Forge 上；
 * 纯规则见 {@link NeoModel}。
 */
public final class NeoMagic {
    public static final String NAMESPACE = "ssca_neomagic";

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, NAMESPACE);
    /** 崩解度三档：失序、瓦解、崩溃。 */
    public static final RegistryObject<MobEffect> DISORDER = EFFECTS.register("disorder", () -> new CollapseEffect(0xC79254, .15));
    public static final RegistryObject<MobEffect> DISINTEGRATION = EFFECTS.register("disintegration", () -> new CollapseEffect(0xA13C63, .35));
    public static final RegistryObject<MobEffect> BREAKDOWN = EFFECTS.register("breakdown", () -> new CollapseEffect.Breakdown(0x681C62, .35));

    private NeoMagic() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(NAMESPACE, path);
    }

    public static void init(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
