package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.init.AddonCriteria;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 附属形态「首次变身」成就：玩家变成 {@value #ADDON_NAMESPACE} 命名空间的形态时触发
 * {@code my_addon:on_transform_addon_form}。登录不会触发（{@link FormChangedEvent} 登录时只记录初始形态）? */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class AddonFormAdvancementHandler {
    /** 附属形态命名空间；只有该命名空间的形态才触发附属变身成就?*/
    private static final String ADDON_NAMESPACE = "my_addon";

    private AddonFormAdvancementHandler() {}

    @SubscribeEvent
    public static void onFormChanged(FormChangedEvent event) {
        ResourceLocation form = event.getNewForm();
        if (form != null && ADDON_NAMESPACE.equals(form.getNamespace())) {
            AddonCriteria.ON_TRANSFORM_ADDON_FORM.trigger(event.getServerPlayer(), form);
        }
    }
}
