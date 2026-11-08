package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.item.BlankFormationPaperItem;
import github.com.gengyoubo.sscfa.item.FormationInkItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SscAddonForge.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SscAddonForge.MOD_ID);

    public static final RegistryObject<Item> CORAL_BALL = ITEMS.register("coral_ball", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FORMATION_MANUSCRIPT = ITEMS.register("formation_manuscript", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FROST_THORN = ITEMS.register("frost_thorn", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MOONLIGHT_ARROW_RENDER = ITEMS.register("moonlight_arrow_render", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLANK_FORMATION_PAPER = ITEMS.register("blank_formation_paper", () -> new BlankFormationPaperItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> INK_NORMAL = ink(FormationInkItem.Type.NORMAL);
    public static final RegistryObject<Item> INK_ICE = ink(FormationInkItem.Type.ICE);
    public static final RegistryObject<Item> INK_FIRE = ink(FormationInkItem.Type.FIRE);
    public static final RegistryObject<Item> INK_LUNAR = ink(FormationInkItem.Type.LUNAR);
    public static final RegistryObject<Item> INK_CURSE = ink(FormationInkItem.Type.CURSE);
    public static final RegistryObject<Item> INK_SUMMON = ink(FormationInkItem.Type.SUMMON);
    public static final RegistryObject<Item> INK_VOID = ink(FormationInkItem.Type.VOID);
    public static final RegistryObject<Item> INK_SPACE = ink(FormationInkItem.Type.SPACE);
    public static final RegistryObject<Item> ENERGY_STORAGE_TANK = ITEMS.register("energy_storage_tank", () ->
            new BlockItem(AddonBlocks.ENERGY_STORAGE_TANK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CREATIVE_ENERGY_TANK = ITEMS.register("creative_energy_tank", () ->
            new BlockItem(AddonBlocks.CREATIVE_ENERGY_TANK.get(), new Item.Properties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("group", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ssc_addon.group"))
            .icon(() -> new ItemStack(CORAL_BALL.get()))
            .displayItems((parameters, output) -> {
                output.accept(CORAL_BALL.get());
                output.accept(FORMATION_MANUSCRIPT.get());
                output.accept(BLANK_FORMATION_PAPER.get());
                output.accept(INK_NORMAL.get());
                output.accept(INK_ICE.get());
                output.accept(INK_FIRE.get());
                output.accept(INK_LUNAR.get());
                output.accept(INK_CURSE.get());
                output.accept(INK_SUMMON.get());
                output.accept(INK_VOID.get());
                output.accept(INK_SPACE.get());
                output.accept(ENERGY_STORAGE_TANK.get());
                output.accept(CREATIVE_ENERGY_TANK.get());
            }).build());

    private static RegistryObject<Item> ink(FormationInkItem.Type type) {
        return ITEMS.register("formation_ink_" + type.id, () -> new FormationInkItem(new Item.Properties().stacksTo(16), type));
    }

    private AddonItems() {}
}
