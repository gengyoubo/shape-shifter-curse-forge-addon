package github.com.gengyoubo.sscfa;

import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.init.AddonEntities;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.init.AddonMenus;
import github.com.gengyoubo.sscfa.init.AddonSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 闂佺绻掗崢褔顢?shim闂佹寧绋掔喊宥嗕繆?Fabric 婵炴垶鎹佸畷鐢垫偖?SscAddon 闂佹眹鍔岀€氭澘顭囬妶澶婄畱濞达絿鎳撻悘鍥煕濞戞鎴澪ｈ娴滄悂宕熼銏㈠帓 Addon* 濠电偛顦崝宀勫船閻ｅ本鍋橀柕濠忕岛閸? * 婵炲濮撮幊鎰板极閵堝棛顩查幖杈剧悼濡炰粙鎮归崶銊ョ仴闁告艾顑囬埀顒€婀卞▍銏㈡濠靛牃鍋撻崷顓炰槐婵＄虎鍨跺畷姘攽閸♀晜缍忛梺鍛婄墬閻楁粓寮?SscAddonForge 闁荤姵鍔楅崰鏇㈡儗濡ゅ懎违? */
public final class SscAddon {
    private SscAddon() {}

    public static final ConcurrentHashMap<UUID, String> PLAYER_LANGUAGES = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> WATER_SPEAR_CRAFT_CD = new ConcurrentHashMap<>();
    public static final int WATER_SPEAR_CRAFT_CD_TICKS = 70;
    public static final Map<UUID, Integer> WS_LAST_SPEAR_COUNT = new ConcurrentHashMap<>();

    public static final MobEffect FOX_FIRE_BURN = AddonEffects.FOX_FIRE_BURN.get();
    public static final MobEffect BLUE_FIRE_RING = AddonEffects.BLUE_FIRE_RING.get();
    public static final MobEffect PLAYING_DEAD = AddonEffects.PLAYING_DEAD.get();
    public static final MobEffect TRUE_INVISIBILITY = AddonEffects.TRUE_INVISIBILITY.get();
    public static final MobEffect PRE_INVISIBILITY = AddonEffects.PRE_INVISIBILITY.get();
    public static final MobEffect STUN = AddonEffects.STUN.get();
    public static final MobEffect FROZEN = AddonEffects.FROZEN.get();
    public static final MobEffect FROZEN_RECOVERY = AddonEffects.FROZEN_RECOVERY.get();
    public static final MobEffect CURSE_MARK = AddonEffects.CURSE_MARK.get();
    public static final MobEffect SHEEP_FORM = AddonEffects.SHEEP_FORM.get();
    public static final MobEffect FROST_SHATTER = AddonEffects.FROST_SHATTER.get();
    public static final MobEffect ROOTED = AddonEffects.ROOTED.get();
    public static final MobEffect GUARANTEED_CRIT = AddonEffects.GUARANTEED_CRIT.get();
    public static final MobEffect FROST_FREEZE = AddonEffects.FROST_FREEZE.get();
    public static final MobEffect FROST_FALL = AddonEffects.FROST_FALL.get();
    public static final MobEffect PURIFIED = AddonEffects.PURIFIED.get();
    public static final MobEffect BAT_REGEN = AddonEffects.BAT_REGEN.get();
    public static final MobEffect BAT_POISON = AddonEffects.BAT_POISON.get();
    public static final MobEffect BAT_ABSORPTION = AddonEffects.BAT_ABSORPTION.get();
    public static final MobEffect FROST_ARMOR = AddonEffects.FROST_ARMOR.get();
    public static final MobEffect MIST_FORM = AddonEffects.MIST_FORM.get();
    public static final MobEffect MIST_CHARGING = AddonEffects.MIST_CHARGING.get();
    public static final MobEffect SAND_BLIND = AddonEffects.SAND_BLIND.get();
    public static final MobEffect DEAFEN = AddonEffects.DEAFEN.get();
    public static final MobEffect TIDAL_SLOW = AddonEffects.TIDAL_SLOW.get();
    public static final MobEffect EROSION_BRAND_MARKER_1 = AddonEffects.EROSION_BRAND_MARKER_1.get();
    public static final MobEffect EROSION_BRAND_MARKER_2 = AddonEffects.EROSION_BRAND_MARKER_2.get();
    public static final MobEffect EROSION_BRAND_MARKER_3 = AddonEffects.EROSION_BRAND_MARKER_3.get();

    public static final Item POTION_BAG = AddonItems.POTION_BAG.get();
    public static final Item MOON_DUST_SPELLBOOK = AddonItems.MOON_DUST_SPELLBOOK.get();
    public static final Item MAGIC_SCROLL = AddonItems.MAGIC_SCROLL.get();
    public static final Item MOONLIGHT_ARROW_RENDER = AddonItems.MOONLIGHT_ARROW_RENDER.get();
    public static final Item FORMATION = AddonItems.FORMATION.get();
    public static final Item FORMATION_MANUSCRIPT = AddonItems.FORMATION_MANUSCRIPT.get();
    public static final Item SPELL_FORMATION = AddonItems.SPELL_FORMATION.get();
    public static final Item ANALYZED_SPELL_DIAGRAM = AddonItems.ANALYZED_SPELL_DIAGRAM.get();
    public static final Item BLANK_FORMATION_PAPER = AddonItems.BLANK_FORMATION_PAPER.get();
    public static final Item FORMATION_INK_NORMAL = AddonItems.INK_NORMAL.get();
    public static final Item FORMATION_INK_ICE = AddonItems.INK_ICE.get();
    public static final Item FORMATION_INK_FIRE = AddonItems.INK_FIRE.get();
    public static final Item FORMATION_INK_LUNAR = AddonItems.INK_LUNAR.get();
    public static final Item FORMATION_INK_CURSE = AddonItems.INK_CURSE.get();
    public static final Item FORMATION_INK_SUMMON = AddonItems.INK_SUMMON.get();
    public static final Item FORMATION_INK_VOID = AddonItems.INK_VOID.get();
    public static final Item FORMATION_INK_SPACE = AddonItems.INK_SPACE.get();
    public static final Item SP_UPGRADE_THING = AddonItems.SP_UPGRADE_THING.get();
    public static final Item INSTINCT_ANCHOR = AddonItems.INSTINCT_ANCHOR.get();
    public static final Item INSTINCT_RELEASE = AddonItems.INSTINCT_RELEASE.get();
    public static final Item PORTABLE_MOISTURIZER = AddonItems.PORTABLE_MOISTURIZER.get();
    public static final Item SNOWBALL_LAUNCHER = AddonItems.SNOWBALL_LAUNCHER.get();
    public static final Item PORTABLE_FRIDGE = AddonItems.PORTABLE_FRIDGE.get();
    public static final Item BLUE_FIRE_AMULET = AddonItems.BLUE_FIRE_AMULET.get();
    public static final Item INVISIBILITY_CLOAK = AddonItems.INVISIBILITY_CLOAK.get();
    public static final Item LIFESAVING_CAT_TAIL = AddonItems.LIFESAVING_CAT_TAIL.get();
    public static final Item PHANTOM_BELL = AddonItems.PHANTOM_BELL.get();
    public static final Item FROST_AMULET = AddonItems.FROST_AMULET.get();
    public static final Item BLOOD_GARNET = AddonItems.BLOOD_GARNET.get();
    public static final Item BLOODLUST_RING = AddonItems.BLOODLUST_RING.get();
    public static final Item HUMUS_RING = AddonItems.HUMUS_RING.get();
    public static final Item TWIN_POD = AddonItems.TWIN_POD.get();
    public static final Item WATER_SPEAR = AddonItems.WATER_SPEAR.get();
    public static final Item FROST_THORN = AddonItems.FROST_THORN.get();
    public static final Item EVOLUTION_STONE = AddonItems.EVOLUTION_STONE.get();
    public static final Item PSIONIC_ORB = AddonItems.PSIONIC_ORB.get();
    public static final Item CORAL_BALL = AddonItems.CORAL_BALL.get();
    public static final Item ACTIVE_CORAL_NECKLACE = AddonItems.ACTIVE_CORAL_NECKLACE.get();
    public static final Item SEA_CRYSTAL_PENDANT = AddonItems.SEA_CRYSTAL_PENDANT.get();
    public static final Item WIND_SPIRIT_STAMINA_NECKLACE = AddonItems.WIND_SPIRIT_STAMINA_NECKLACE.get();
    public static final Item NOVA_REVIVE_NECKLACE = AddonItems.NOVA_REVIVE_NECKLACE.get();
    public static final Item NIGHTMARE_RING = AddonItems.NIGHTMARE_RING.get();
    public static final Item FROST_SPINE_COLLAR = AddonItems.FROST_SPINE_COLLAR.get();
    public static final Item VENOM_GLAND = AddonItems.VENOM_GLAND.get();
    public static final Item ANUBIS_CRYSTAL = AddonItems.ANUBIS_CRYSTAL.get();
    public static final Item ANKH_STONE = AddonItems.ANKH_STONE.get();
    public static final Item BINDING_ANKLET = AddonItems.BINDING_ANKLET.get();
    public static final Item EROSION_SAND_PRISM = AddonItems.EROSION_SAND_PRISM.get();
    public static final Item WITHERED_SAND_RING = AddonItems.WITHERED_SAND_RING.get();
    public static final Item ALLAY_HEAL_WAND = AddonItems.ALLAY_HEAL_WAND.get();
    public static final Item ALLAY_JUKEBOX = AddonItems.ALLAY_JUKEBOX.get();
    public static final Item FRIEND_MARKER = AddonItems.FRIEND_MARKER.get();
    public static final Item CLEAR_FRIEND_MARKER = AddonItems.CLEAR_FRIEND_MARKER.get();
    public static final Item WITCH_FAMILIAR_SPAWN_EGG = AddonItems.WITCH_FAMILIAR_SPAWN_EGG.get();
    public static final Item AXOLOTL_SHIFTER_SPAWN_EGG = AddonItems.AXOLOTL_SHIFTER_SPAWN_EGG.get();
    public static final Item INFINITE_ENERGY_POTION = AddonItems.INFINITE_ENERGY_POTION.get();
    public static final Item INFINITE_ENERGY_POTION_SPLASH = AddonItems.INFINITE_ENERGY_POTION_SPLASH.get();
    public static final Item INFINITE_ENERGY_POTION_LINGERING = AddonItems.INFINITE_ENERGY_POTION_LINGERING.get();
    public static final Item UNIVERSAL_ENERGY_POTION = AddonItems.UNIVERSAL_ENERGY_POTION.get();
    public static final Item UNIVERSAL_ENERGY_POTION_SPLASH = AddonItems.UNIVERSAL_ENERGY_POTION_SPLASH.get();
    public static final Item UNIVERSAL_ENERGY_POTION_LINGERING = AddonItems.UNIVERSAL_ENERGY_POTION_LINGERING.get();
    public static final Item WITHER_POTION = AddonItems.WITHER_POTION.get();
    public static final Item WITHER_POTION_SPLASH = AddonItems.WITHER_POTION_SPLASH.get();
    public static final Item WITHER_POTION_LINGERING = AddonItems.WITHER_POTION_LINGERING.get();
    public static final Item MUSIC_DISC_SHAPE_SHIFTERS_DREAM = AddonItems.MUSIC_DISC_SHAPE_SHIFTERS_DREAM.get();

    public static final EntityType<github.com.gengyoubo.sscfa.entity.FrostBallEntity> FROST_BALL_ENTITY = null;
    public static final EntityType<?> SPELL_FROST_SPIKE_ENTITY = AddonEntities.SPELL_FROST_SPIKE.get();
    public static final EntityType<?> SPELL_FIRE_BOLT_ENTITY = AddonEntities.SPELL_FIRE_BOLT.get();
    public static final EntityType<?> SPELL_METEOR_ENTITY = AddonEntities.SPELL_METEOR.get();
    public static final EntityType<?> SPELL_MOONLIGHT_ARROW_ENTITY = AddonEntities.SPELL_MOONLIGHT_ARROW.get();
    public static final EntityType<?> SPELL_CURSE_MARK_ENTITY = AddonEntities.SPELL_CURSE_MARK.get();
    public static final EntityType<?> BEEP_SHEEP_ENTITY = AddonEntities.BEEP_SHEEP.get();
    public static final EntityType<github.com.gengyoubo.sscfa.entity.LunarSpiritEntity> LUNAR_SPIRIT_ENTITY = null;
    public static final EntityType<github.com.gengyoubo.sscfa.entity.LunarSpiritBoltEntity> LUNAR_SPIRIT_BOLT_ENTITY = null;
    public static final EntityType<github.com.gengyoubo.sscfa.entity.ThrownWaterSpearEntity> THROWN_WATER_SPEAR_ENTITY = null;
    public static final EntityType<?> FROST_THORN_ENTITY = AddonEntities.FROST_THORN.get();
    public static final EntityType<?> FROST_ARRAY_ENTITY = AddonEntities.FROST_ARRAY.get();
    public static final EntityType<github.com.gengyoubo.sscfa.entity.FoxFireballEntity> FOX_FIREBALL_ENTITY = null;
    public static final EntityType<github.com.gengyoubo.sscfa.entity.InfectionSporeBombEntity> INFECTION_SPORE_BOMB_ENTITY = null;
    public static final EntityType<github.com.gengyoubo.sscfa.entity.ParasiticSeedProjectile> PARASITIC_SEED_ENTITY = null;
    public static final EntityType<?> FROST_STORM_ENTITY = AddonEntities.FROST_STORM.get();
    public static final EntityType<?> TIDAL_ORB_ENTITY = AddonEntities.TIDAL_ORB.get();
    public static final EntityType<?> LASER_BEAM_ENTITY = AddonEntities.LASER_BEAM.get();
    public static final EntityType<github.com.gengyoubo.sscfa.item.WaterSpearEntity> WATER_SPEAR_ENTITY = null;
    public static final EntityType<?> GHOST_CAT_ENTITY = AddonEntities.GHOST_CAT.get();
    public static final EntityType<github.com.gengyoubo.sscfa.entity.AllayFriendMarkerEntity> FRIEND_MARKER_ENTITY_TYPE = null;
    public static final EntityType<github.com.gengyoubo.sscfa.entity.AllayClearMarkerEntity> CLEAR_MARKER_ENTITY_TYPE = null;
    public static final EntityType<?> WITCH_FAMILIAR_ENTITY = AddonEntities.WITCH_FAMILIAR.get();
    public static final EntityType<?> AXOLOTL_SHIFTER_ENTITY = AddonEntities.AXOLOTL_SHIFTER.get();

    public static final SoundEvent SHAPE_SHIFTERS_DREAM_EVENT = AddonSounds.SHAPE_SHIFTERS_DREAM.get();
    public static final ResourceLocation SHAPE_SHIFTERS_DREAM_ID = AddonSounds.SHAPE_SHIFTERS_DREAM.getId();
    public static final SoundEvent ALLAY_HEAL_MUSIC_EVENT = AddonSounds.ALLAY_HEAL_MUSIC.get();
    public static final ResourceLocation ALLAY_HEAL_MUSIC_ID = AddonSounds.ALLAY_HEAL_MUSIC.getId();
    public static final SoundEvent ALLAY_SPEED_MUSIC_EVENT = AddonSounds.ALLAY_SPEED_MUSIC.get();
    public static final ResourceLocation ALLAY_SPEED_MUSIC_ID = AddonSounds.ALLAY_SPEED_MUSIC.getId();
    public static final SoundEvent EXPLOSION_THEME_EVENT = AddonSounds.EXPLOSION_THEME.get();
    public static final ResourceLocation EXPLOSION_THEME_ID = AddonSounds.EXPLOSION_THEME.getId();

    public static final MenuType<?> POTION_BAG_SCREEN_HANDLER = AddonMenus.POTION_BAG_SCREEN_HANDLER.get();
    public static final MenuType<?> SPELLBOOK_SCREEN_HANDLER = AddonMenus.SPELLBOOK_SCREEN_HANDLER.get();
    public static final Potion UNIVERSAL_ENERGY_POTION_TYPE = null;
    public static final RecipeSerializer<?> REFILL_MOISTURIZER_SERIALIZER = null;
    public static final RecipeSerializer<?> UPGRADE_MOISTURIZER_SERIALIZER = null;
    public static final RecipeSerializer<?> RELOAD_SNOWBALL_LAUNCHER_SERIALIZER = null;
    public static final RecipeSerializer<?> BLIZZARD_TANK_RECHARGE_SERIALIZER = null;
    public static final RecipeSerializer<?> SP_UPGRADE_SERIALIZER = null;
    public static final RecipeSerializer<?> INFINITE_ENERGY_POTION_SERIALIZER = null;
    public static final RecipeSerializer<?> VENOM_GLAND_SERIALIZER = null;
    public static final Object ON_TRANSFORM_ADDON_FORM = null;

    public static void onInitialize() {}
    public static void registerApoliSystems() {}
    public static void registerEntitySpawnHandlers() {}
    public static void registerForms() {}
    public static void registerMancianimaEvents() {}
    public static void registerPlayerEventHandlers() {}
}
