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
 * 閺備即鐡熷▔鏇犻兇缂佺喎鍙嗛崣锝忕礄閸涜棄鎮曠粚娲？ {@value #NAMESPACE}閿涘鈧倹鏌婃鏃€纭堕惃鍕暈閸愬被鈧焦鏆熼幑顕€鍣告潪濮愨偓浣虹秹缂佹粌鎷版潻鎰攽閺冨爼鍏樻禒搴ょ箹闁插本瀵曢崚?Forge 娑撳绱?
 * 鐟欏嫬鍨痪锕€鐣剧憴?{@link INeoModel}閿涘矂绮拋銈堫潐閸掓瑨顫?{@link NeoModel}閿涘本鏆熼幑顔煎瘶鐟欏嫬鍨憴?{@link NeoRules}閵?
 */
public final class NeoMagic {
    public static final String NAMESPACE = "ssca_neomagic";
    public static final Logger LOG = LoggerFactory.getLogger(NAMESPACE);

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, NAMESPACE);
    /** 瀹曗晞袙鎼达缚绗佸锝忕窗婢跺崬绨妴浣烘憹鐟欙絻鈧礁绌垮┃鍐︹偓?*/
    public static final RegistryObject<MobEffect> DISORDER = EFFECTS.register("disorder", () -> new CollapseEffect(0xC79254, .15));
    public static final RegistryObject<MobEffect> DISINTEGRATION = EFFECTS.register("disintegration", () -> new CollapseEffect(0xA13C63, .35));
    public static final RegistryObject<MobEffect> BREAKDOWN = EFFECTS.register("breakdown", () -> new CollapseEffect.Breakdown(0x681C62, .35));

    private NeoMagic() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(NAMESPACE, path);
    }

    /** 濡紕绮嶉悧鍫熸拱閿涙稑绱戦崣鎴犲箚婢у啯鍨ㄥù瀣槸娑擃厽瀣佹稉宥呭煂閺冩湹璐?{@code "test"}閵?*/
    public static String engineVersion() {
        ModList mods = ModList.get();
        if (mods == null) return "test";
        return mods.getModContainerById(SscAddonForge.MOD_ID).map(c -> c.getModInfo().getVersion().toString()).orElse("test");
    }

    public static net.minecraft.network.chat.MutableComponent text(String key, Object... args) {
        return net.minecraft.network.chat.Component.translatable("ssca_neomagic." + key, args);
    }

    public static void init(IEventBus modBus) {
        EFFECTS.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(NeoMagic::addReloadListeners);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new NeoRulesLoader());
    }
}
