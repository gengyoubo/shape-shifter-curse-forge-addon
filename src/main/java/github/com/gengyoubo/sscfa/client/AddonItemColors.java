package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.FormationElement;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.util.RenderContextTracker;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 物品染色与模型谓词（移植自 Fabric 版 {@code SscAddonClient} / {@code NeoClient}）。
 * 卷轴与法阵是双层模型：layer0 纸体不染，layer1 图案按系别色染（无系别为灰白）；
 * 药水类物品 layer0 是液体层。
 */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AddonItemColors {
    private static final int BLANK_PATTERN = 0xB8B8B8;
    private static final int WITHER_LIQUID = 0x4A403A;
    private static final int MOON_DUST_COLOR = 0xA7EDF5;

    private AddonItemColors() {}

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // 魔法卷轴与增强法阵：按法术 JSON 的 element 染图案层。
        event.register((stack, tint) -> tint != 1 ? -1 : scrollColor(stack),
                AddonItems.MAGIC_SCROLL.get(), AddonItems.SPELL_FORMATION.get());
        // 法阵：按 NBT Element 染图案层（通用系由 universal 谓词切到单层模型）。
        event.register((stack, tint) -> tint != 1 ? -1 : color(FormationData.getElement(stack)),
                AddonItems.FORMATION.get());
        // 药水试管：液体层取药水颜色。
        event.register((stack, tint) -> tint == 0 ? PotionUtils.getColor(stack) : -1,
                AddonItems.TEST_TUBE_POTION.get());
        // 月尘舒缓剂：液体层默认月尘色，带 CustomPotionColor 时用其颜色。
        event.register((stack, tint) -> tint != 0 ? -1
                        : stack.hasTag() && stack.getTag().contains("CustomPotionColor") ? PotionUtils.getColor(stack) : MOON_DUST_COLOR,
                AddonItems.MOON_DUST_VIAL.get(), AddonItems.MOON_DUST_BOTTLE.get(), AddonItems.MOON_DUST_SPLASH.get());
        // 凋零药水：液体层染凋零色，避免空瓶外观。
        event.register((stack, tint) -> tint == 0 ? WITHER_LIQUID : -1,
                AddonItems.WITHER_POTION.get(), AddonItems.WITHER_POTION_SPLASH.get(), AddonItems.WITHER_POTION_LINGERING.get());
    }

    /** 在客户端初始化的主线程任务中调用。 */
    public static void registerItemProperties() {
        ItemProperties.register(AddonItems.FORMATION.get(), id("universal"), (stack, world, entity, seed) ->
                FormationData.getElement(stack) == FormationElement.UNIVERSAL ? 1.0F : 0.0F);
        // 手持时切换为 3D 模型；物品栏/掉落物（GUI 上下文）始终用 2D 图标。
        ItemProperties.register(AddonItems.WATER_SPEAR.get(), id("held"), (stack, world, entity, seed) ->
                RenderContextTracker.isGuiContext() ? 0.0F
                        : entity != null && (entity.getMainHandItem() == stack || entity.getOffhandItem() == stack) ? 1.0F : 0.0F);
        ItemProperties.register(AddonItems.WATER_SPEAR.get(), id("throwing"), (stack, world, entity, seed) ->
                RenderContextTracker.isGuiContext() ? 0.0F
                        : entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    private static int scrollColor(ItemStack stack) {
        Spell spell = ScrollData.getSpell(stack);
        if (spell == null) return BLANK_PATTERN;
        return color(FormationElement.byId(spell.getConfig().element));
    }

    private static int color(FormationElement element) {
        return element == null ? BLANK_PATTERN : element.color;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SscAddonForge.MOD_ID, path);
    }
}
