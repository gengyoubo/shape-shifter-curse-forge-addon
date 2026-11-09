package github.com.gengyoubo.sscfa.neomagic;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 新魔法系统入口（命名空间 {@value #NAMESPACE}）。新魔法的注册、数据重载、网络和运行时都从这里挂到 Forge 上；
 * 规则约定见 {@link INeoModel}，默认规则见 {@link NeoModel}，数据包规则见 {@link NeoRules}。
 */
public final class NeoMagic {
    public static final String NAMESPACE = "ssca_neomagic";
    public static final Logger LOG = LoggerFactory.getLogger(NAMESPACE);

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, NAMESPACE);
    /** 崩解度三档：失序、瓦解、崩溃。 */
    public static final RegistryObject<MobEffect> DISORDER = EFFECTS.register("disorder", () -> new CollapseEffect(0xC79254, .15));
    public static final RegistryObject<MobEffect> DISINTEGRATION = EFFECTS.register("disintegration", () -> new CollapseEffect(0xA13C63, .35));
    public static final RegistryObject<MobEffect> BREAKDOWN = EFFECTS.register("breakdown", () -> new CollapseEffect.Breakdown(0x681C62, .35));

    private NeoMagic() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(NAMESPACE, path);
    }

    /** 模组版本；开发环境或测试中拿不到时为 {@code "test"}。 */
    public static String engineVersion() {
        ModList mods = ModList.get();
        if (mods == null) return "test";
        return mods.getModContainerById(SscAddonForge.MOD_ID).map(c -> c.getModInfo().getVersion().toString()).orElse("test");
    }

    public static void init(IEventBus modBus) {
        EFFECTS.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(NeoMagic::addReloadListeners);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new NeoRulesLoader());
    }
}
