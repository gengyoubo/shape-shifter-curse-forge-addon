package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.item.BlankFormationPaperItem;
import github.com.gengyoubo.sscfa.item.FormationInkItem;
import github.com.gengyoubo.sscfa.item.UniversalEnergyPotionItem;
import github.com.gengyoubo.sscfa.item.InfiniteEnergyPotionItem;
import github.com.gengyoubo.sscfa.item.WitherPotionItem;
import github.com.gengyoubo.sscfa.item.PortableFridgeItem;
import github.com.gengyoubo.sscfa.item.PortableMoisturizerItem;
import github.com.gengyoubo.sscfa.item.PotionTestTubes;
import github.com.gengyoubo.sscfa.item.SnowballLauncherItem;
import github.com.gengyoubo.sscfa.item.WaterSpearItem;
import github.com.gengyoubo.sscfa.item.PsionicOrbItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
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

    // ===== 物品与饰品 =====
    public static final RegistryObject<Item> INSTINCT_ANCHOR = ITEMS.register("instinct_anchor", () -> new github.com.gengyoubo.sscfa.item.InstinctAnchorItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), true));
    public static final RegistryObject<Item> INSTINCT_RELEASE = ITEMS.register("instinct_release", () -> new github.com.gengyoubo.sscfa.item.InstinctAnchorItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), false));
    public static final RegistryObject<Item> POTION_BAG = ITEMS.register("potion_bag", () -> new github.com.gengyoubo.sscfa.item.PotionBagItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MOON_DUST_SPELLBOOK = ITEMS.register("moon_dust_spellbook", () -> new github.com.gengyoubo.sscfa.item.MoonDustSpellbookItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MAGIC_SCROLL = ITEMS.register("magic_scroll", () -> new github.com.gengyoubo.sscfa.item.MagicScrollItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> FORMATION = ITEMS.register("formation", () -> new github.com.gengyoubo.sscfa.item.FormationItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SPELL_FORMATION = ITEMS.register("spell_formation", () -> new github.com.gengyoubo.sscfa.item.SpellFormationItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ANALYZED_SPELL_DIAGRAM = ITEMS.register("analyzed_spell_diagram", () -> new github.com.gengyoubo.sscfa.item.AnalyzedSpellDiagramItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PORTABLE_MOISTURIZER = ITEMS.register("portable_moisturizer", () -> new PortableMoisturizerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SNOWBALL_LAUNCHER = ITEMS.register("snowball_launcher", () -> new SnowballLauncherItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PORTABLE_FRIDGE = ITEMS.register("portable_fridge", () -> new PortableFridgeItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BLUE_FIRE_AMULET = ITEMS.register("blue_fire_amulet", () -> new github.com.gengyoubo.sscfa.item.BlueFireAmuletItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> FROST_AMULET = ITEMS.register("frost_amulet", () -> new github.com.gengyoubo.sscfa.item.FrostAmuletItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> INVISIBILITY_CLOAK = ITEMS.register("invisibility_cloak", () -> new github.com.gengyoubo.sscfa.item.InvisibilityCloakItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> LIFESAVING_CAT_TAIL = ITEMS.register("lifesaving_cat_tail", () -> new github.com.gengyoubo.sscfa.item.LifesavingCatTailItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> PHANTOM_BELL = ITEMS.register("phantom_bell", () -> new github.com.gengyoubo.sscfa.item.PhantomBellItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> BLOOD_GARNET = ITEMS.register("blood_garnet", () -> new github.com.gengyoubo.sscfa.item.BloodGarnetItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> BLOODLUST_RING = ITEMS.register("bloodlust_ring", () -> new github.com.gengyoubo.sscfa.item.BloodlustRingItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> HUMUS_RING = ITEMS.register("humus_ring", () -> new github.com.gengyoubo.sscfa.item.HumusRingItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> TWIN_POD = ITEMS.register("twin_pod", () -> new github.com.gengyoubo.sscfa.item.TwinPodItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> WATER_SPEAR = ITEMS.register("water_spear", () -> new WaterSpearItem(new Item.Properties().durability(60)));
    public static final RegistryObject<Item> EVOLUTION_STONE = ITEMS.register("evolution_stone", () -> new github.com.gengyoubo.sscfa.item.EvolutionStoneItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> PSIONIC_ORB = ITEMS.register("psionic_orb", () -> new PsionicOrbItem(new Item.Properties().stacksTo(16).fireResistant()));
    public static final RegistryObject<Item> ACTIVE_CORAL_NECKLACE = ITEMS.register("active_coral_necklace", () -> new github.com.gengyoubo.sscfa.item.ActiveCoralNecklaceItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SEA_CRYSTAL_PENDANT = ITEMS.register("sea_crystal_pendant", () -> new github.com.gengyoubo.sscfa.item.SeaCrystalPendantItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WIND_SPIRIT_STAMINA_NECKLACE = ITEMS.register("wind_spirit_stamina_necklace", () -> new github.com.gengyoubo.sscfa.item.WindSpiritStaminaNecklaceItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NOVA_REVIVE_NECKLACE = ITEMS.register("nova_revive_necklace", () -> new github.com.gengyoubo.sscfa.item.NovaReviveNecklaceItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ANUBIS_CRYSTAL = ITEMS.register("anubis_crystal", () -> new github.com.gengyoubo.sscfa.item.AnubisCrystalItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> ANKH_STONE = ITEMS.register("ankh_stone", () -> new github.com.gengyoubo.sscfa.item.AnkhStoneItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> BINDING_ANKLET = ITEMS.register("binding_anklet", () -> new github.com.gengyoubo.sscfa.item.BindingAnkletItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> EROSION_SAND_PRISM = ITEMS.register("erosion_sand_prism", () -> new github.com.gengyoubo.sscfa.item.ErosionSandPrismItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> WITHERED_SAND_RING = ITEMS.register("withered_sand_ring", () -> new github.com.gengyoubo.sscfa.item.WitheredSandRingItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> NIGHTMARE_RING = ITEMS.register("nightmare_ring", () -> new github.com.gengyoubo.sscfa.item.NightmareRingItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> FROST_SPINE_COLLAR = ITEMS.register("frost_spine_collar", () -> new github.com.gengyoubo.sscfa.item.FrostSpineCollarItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> VENOM_GLAND = ITEMS.register("venom_gland", () -> new github.com.gengyoubo.sscfa.item.VenomGlandItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> ALLAY_HEAL_WAND = ITEMS.register("allay_heal_wand", () -> new github.com.gengyoubo.sscfa.item.AllayHealWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ALLAY_JUKEBOX = ITEMS.register("allay_jukebox", () -> new github.com.gengyoubo.sscfa.item.AllayJukeboxItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FRIEND_MARKER = ITEMS.register("friend_marker", () -> new github.com.gengyoubo.sscfa.item.AllayFriendMarkerItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> CLEAR_FRIEND_MARKER = ITEMS.register("clear_friend_marker", () -> new github.com.gengyoubo.sscfa.item.AllayClearMarkerItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> WITHER_POTION = ITEMS.register("wither_potion", () -> new WitherPotionItem(new Item.Properties().stacksTo(1), WitherPotionItem.Type.DRINK));
    public static final RegistryObject<Item> WITHER_POTION_SPLASH = ITEMS.register("wither_potion_splash", () -> new WitherPotionItem(new Item.Properties().stacksTo(1), WitherPotionItem.Type.SPLASH));
    public static final RegistryObject<Item> WITHER_POTION_LINGERING = ITEMS.register("wither_potion_lingering", () -> new WitherPotionItem(new Item.Properties().stacksTo(1), WitherPotionItem.Type.LINGERING));
    public static final RegistryObject<Item> MOON_DUST_VIAL = ITEMS.register("moon_dust_vial",
            () -> github.com.gengyoubo.sscfa.item.MoonDustReliefItem.vial());
    public static final RegistryObject<Item> MOON_DUST_BOTTLE = ITEMS.register("moon_dust_bottle",
            () -> github.com.gengyoubo.sscfa.item.MoonDustReliefItem.bottle());
    public static final RegistryObject<Item> MOON_DUST_SPLASH = ITEMS.register("moon_dust_splash",
            () -> github.com.gengyoubo.sscfa.item.MoonDustReliefItem.splash());
    public static final RegistryObject<Item> TEST_TUBE = ITEMS.register("test_tube", () -> new Item(new Item.Properties().stacksTo(64)));
    /** 装药试管：一瓶分三支，饮用按 1/3 剂量（见 PotionTestTubes）。 */
    public static final RegistryObject<Item> TEST_TUBE_POTION = ITEMS.register("test_tube_potion", () -> new PotionTestTubes.FilledTube(new Item.Properties().stacksTo(PotionTestTubes.DEFAULT_STACK_LIMIT)));
    public static final RegistryObject<Item> MUSIC_DISC_SHAPE_SHIFTERS_DREAM = ITEMS.register("music_disc_shape_shifters_dream", () ->
            new RecordItem(15, AddonSounds.SHAPE_SHIFTERS_DREAM, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 145 * 20));
    public static final RegistryObject<Item> WITCH_FAMILIAR_SPAWN_EGG = ITEMS.register("witch_familiar_spawn_egg", () ->
            new ForgeSpawnEggItem(AddonEntities.WITCH_FAMILIAR, 0xF0A81E, 0x31C8CC, new Item.Properties()));
    public static final RegistryObject<Item> AXOLOTL_SHIFTER_SPAWN_EGG = ITEMS.register("axolotl_shifter_spawn_egg", () ->
            new ForgeSpawnEggItem(AddonEntities.AXOLOTL_SHIFTER, 0xFBC1E3, 0xA6DCF0, new Item.Properties()));
    public static final RegistryObject<Item> WEB_MEMBRANE = ITEMS.register("web_membrane", () ->
            new BlockItem(AddonBlocks.WEB_MEMBRANE.get(), new Item.Properties()));
    public static final RegistryObject<Item> INFUSION_ALTAR = ITEMS.register("infusion_altar", () ->
            new BlockItem(AddonBlocks.INFUSION_ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPELL_RESEARCH_TABLE = ITEMS.register("spell_research_table", () ->
            new BlockItem(AddonBlocks.SPELL_RESEARCH_TABLE.get(), new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("group", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ssc_addon.group"))
            .icon(() -> new ItemStack(SP_UPGRADE_THING.get()))
            .displayItems((parameters, output) -> {
                output.accept(SP_UPGRADE_THING.get());
                output.accept(INSTINCT_ANCHOR.get());
                output.accept(INSTINCT_RELEASE.get());
                output.accept(MOON_DUST_SPELLBOOK.get());
                output.accept(INK_NORMAL.get());
                output.accept(INK_ICE.get());
                output.accept(INK_FIRE.get());
                output.accept(INK_LUNAR.get());
                output.accept(INK_CURSE.get());
                output.accept(INK_SUMMON.get());
                output.accept(INK_VOID.get());
                output.accept(INK_SPACE.get());
                output.accept(BLANK_FORMATION_PAPER.get());
                output.accept(FORMATION_MANUSCRIPT.get());
                output.accept(EVOLUTION_STONE.get());
                output.accept(PSIONIC_ORB.get());
                output.accept(LIFESAVING_CAT_TAIL.get());
                output.accept(PHANTOM_BELL.get());
                output.accept(FROST_AMULET.get());
                output.accept(BLUE_FIRE_AMULET.get());
                output.accept(INVISIBILITY_CLOAK.get());
                output.accept(PORTABLE_MOISTURIZER.get());
                output.accept(PORTABLE_FRIDGE.get());
                output.accept(SNOWBALL_LAUNCHER.get());
                output.accept(WATER_SPEAR.get());
                output.accept(CORAL_BALL.get());
                output.accept(ACTIVE_CORAL_NECKLACE.get());
                output.accept(SEA_CRYSTAL_PENDANT.get());
                output.accept(WIND_SPIRIT_STAMINA_NECKLACE.get());
                output.accept(NOVA_REVIVE_NECKLACE.get());
                output.accept(ANUBIS_CRYSTAL.get());
                output.accept(ANKH_STONE.get());
                output.accept(BINDING_ANKLET.get());
                output.accept(EROSION_SAND_PRISM.get());
                output.accept(WITHERED_SAND_RING.get());
                output.accept(NIGHTMARE_RING.get());
                output.accept(FROST_SPINE_COLLAR.get());
                output.accept(VENOM_GLAND.get());
                output.accept(BLOOD_GARNET.get());
                output.accept(BLOODLUST_RING.get());
                output.accept(HUMUS_RING.get());
                output.accept(TWIN_POD.get());
                output.accept(ALLAY_HEAL_WAND.get());
                output.accept(ALLAY_JUKEBOX.get());
                output.accept(MUSIC_DISC_SHAPE_SHIFTERS_DREAM.get());
                output.accept(FRIEND_MARKER.get());
                output.accept(CLEAR_FRIEND_MARKER.get());
                output.accept(WITCH_FAMILIAR_SPAWN_EGG.get());
                output.accept(AXOLOTL_SHIFTER_SPAWN_EGG.get());
                output.accept(INFINITE_ENERGY_POTION.get());
                output.accept(INFINITE_ENERGY_POTION_SPLASH.get());
                output.accept(INFINITE_ENERGY_POTION_LINGERING.get());
                output.accept(UNIVERSAL_ENERGY_POTION.get());
                output.accept(UNIVERSAL_ENERGY_POTION_SPLASH.get());
                output.accept(UNIVERSAL_ENERGY_POTION_LINGERING.get());
                output.accept(WITHER_POTION.get());
                output.accept(WITHER_POTION_SPLASH.get());
                output.accept(WITHER_POTION_LINGERING.get());
                output.accept(WEB_MEMBRANE.get());
                output.accept(ENERGY_EXTRACTOR.get());
                output.accept(ENERGY_STORAGE_TANK.get());
                output.accept(CREATIVE_ENERGY_TANK.get());
                output.accept(ENERGY_CONVERTER.get());
                output.accept(ENERGY_BOTTLER.get());
                output.accept(POTION_STORAGE_BOX.get());
                output.accept(INFUSION_ALTAR.get());
                output.accept(SPELL_RESEARCH_TABLE.get());
            }).build());

    /** 法术页：法术卷轴与增强法阵的全套等级需法术系统生成，迁移前只放材料与药剂。 */
    public static final RegistryObject<CreativeModeTab> SPELL_TAB = TABS.register("spells", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ssc_addon.spells"))
            .icon(() -> new ItemStack(BLANK_FORMATION_PAPER.get()))
            .withTabsBefore(TAB.getId())
            .displayItems((parameters, output) -> {
                output.accept(TEST_TUBE.get());
                output.accept(MOON_DUST_VIAL.get());
                output.accept(MOON_DUST_BOTTLE.get());
                output.accept(MOON_DUST_SPLASH.get());
                output.accept(MAGIC_SCROLL.get());
                output.accept(FORMATION.get());
                output.accept(SPELL_FORMATION.get());
                output.accept(ANALYZED_SPELL_DIAGRAM.get());
            }).build());

    private static RegistryObject<Item> ink(FormationInkItem.Type type) {
        return ITEMS.register("formation_ink_" + type.id, () -> new FormationInkItem(new Item.Properties().stacksTo(16), type));
    }

    private AddonItems() {}
}
