package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.item.BlankFormationPaperItem;
import github.com.gengyoubo.sscfa.item.FormationInkItem;
import github.com.gengyoubo.sscfa.item.UniversalEnergyPotionItem;
import github.com.gengyoubo.sscfa.item.InfiniteEnergyPotionItem;
import github.com.gengyoubo.sscfa.moonmarrow.SpUpgradeItem;
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
    public static final RegistryObject<Item> SP_UPGRADE_THING = ITEMS.register("sp_upgrade_thing", () -> new SpUpgradeItem(new Item.Properties().stacksTo(1)));
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
    public static final RegistryObject<Item> ENERGY_CONVERTER = ITEMS.register("energy_converter", () ->
            new BlockItem(AddonBlocks.ENERGY_CONVERTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> ENERGY_EXTRACTOR = ITEMS.register("energy_extractor", () ->
            new BlockItem(AddonBlocks.ENERGY_EXTRACTOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ENERGY_BOTTLER = ITEMS.register("energy_bottler", () ->
            new BlockItem(AddonBlocks.ENERGY_BOTTLER.get(), new Item.Properties()));
    public static final RegistryObject<Item> POTION_STORAGE_BOX = ITEMS.register("potion_storage_box", () ->
            new BlockItem(AddonBlocks.POTION_STORAGE_BOX.get(), new Item.Properties()));
    public static final RegistryObject<Item> UNIVERSAL_ENERGY_POTION = ITEMS.register("universal_energy_potion", () ->
            new UniversalEnergyPotionItem(new Item.Properties().stacksTo(1), UniversalEnergyPotionItem.Type.DRINK));
    public static final RegistryObject<Item> UNIVERSAL_ENERGY_POTION_SPLASH = ITEMS.register("universal_energy_potion_splash", () ->
            new UniversalEnergyPotionItem(new Item.Properties().stacksTo(1), UniversalEnergyPotionItem.Type.SPLASH));
    public static final RegistryObject<Item> UNIVERSAL_ENERGY_POTION_LINGERING = ITEMS.register("universal_energy_potion_lingering", () ->
            new UniversalEnergyPotionItem(new Item.Properties().stacksTo(1), UniversalEnergyPotionItem.Type.LINGERING));
    public static final RegistryObject<Item> INFINITE_ENERGY_POTION = ITEMS.register("infinite_energy_potion", () ->
            new InfiniteEnergyPotionItem(new Item.Properties().stacksTo(1), InfiniteEnergyPotionItem.Type.DRINK));
    public static final RegistryObject<Item> INFINITE_ENERGY_POTION_SPLASH = ITEMS.register("infinite_energy_potion_splash", () ->
            new InfiniteEnergyPotionItem(new Item.Properties().stacksTo(1), InfiniteEnergyPotionItem.Type.SPLASH));
    public static final RegistryObject<Item> INFINITE_ENERGY_POTION_LINGERING = ITEMS.register("infinite_energy_potion_lingering", () ->
            new InfiniteEnergyPotionItem(new Item.Properties().stacksTo(1), InfiniteEnergyPotionItem.Type.LINGERING));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("group", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ssc_addon.group"))
            .icon(() -> new ItemStack(CORAL_BALL.get()))
            .displayItems((parameters, output) -> {
                output.accept(SP_UPGRADE_THING.get());
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
                output.accept(ENERGY_CONVERTER.get());
                output.accept(ENERGY_EXTRACTOR.get());
                output.accept(ENERGY_BOTTLER.get());
                output.accept(POTION_STORAGE_BOX.get());
                output.accept(UNIVERSAL_ENERGY_POTION.get());
                output.accept(UNIVERSAL_ENERGY_POTION_SPLASH.get());
                output.accept(UNIVERSAL_ENERGY_POTION_LINGERING.get());
                output.accept(INFINITE_ENERGY_POTION.get());
                output.accept(INFINITE_ENERGY_POTION_SPLASH.get());
                output.accept(INFINITE_ENERGY_POTION_LINGERING.get());
            }).build());

    private static RegistryObject<Item> ink(FormationInkItem.Type type) {
        return ITEMS.register("formation_ink_" + type.id, () -> new FormationInkItem(new Item.Properties().stacksTo(16), type));
    }

    private AddonItems() {}
}
