package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.effect.*;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.function.Supplier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SscAddonForge.MOD_ID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, SscAddonForge.MOD_ID);
    public static final RegistryObject<MobEffect> UNIVERSAL_ENERGY = EFFECTS.register("universal_energy", UniversalEnergyEffect::new);
    public static final RegistryObject<Potion> UNIVERSAL_ENERGY_POTION = POTIONS.register("universal_energy", () ->
            new Potion(new MobEffectInstance(UNIVERSAL_ENERGY.get(), 1)));
    public static final int MOON_DUST_COLOR = 0xA7EDF5;

    // ===== 已完整移植 =====
    public static final RegistryObject<MobEffect> STUN = EFFECTS.register("stun", StunEffect::new);
    public static final RegistryObject<MobEffect> FROZEN = EFFECTS.register("frozen", FrozenEffect::new);
    public static final RegistryObject<MobEffect> FROZEN_RECOVERY = EFFECTS.register("frozen_recovery", FrozenRecoveryEffect::new);
    public static final RegistryObject<MobEffect> ROOTED = EFFECTS.register("rooted", RootedEffect::new);
    public static final RegistryObject<MobEffect> GUARANTEED_CRIT = EFFECTS.register("guaranteed_crit", GuaranteedCritEffect::new);
    public static final RegistryObject<MobEffect> BAT_REGEN = EFFECTS.register("bat_regen", BatRegenEffect::new);
    public static final RegistryObject<MobEffect> BAT_POISON = EFFECTS.register("bat_poison", BatPoisonEffect::new);
    public static final RegistryObject<MobEffect> BAT_ABSORPTION = EFFECTS.register("bat_absorption", BatAbsorptionEffect::new);
    public static final RegistryObject<MobEffect> FROST_ARMOR = EFFECTS.register("frost_armor", FrostArmorEffect::new);
    public static final RegistryObject<MobEffect> MIST_FORM = EFFECTS.register("mist_form", MistFormEffect::new);
    public static final RegistryObject<MobEffect> MIST_CHARGING = EFFECTS.register("mist_charging", MistChargingEffect::new);
    public static final RegistryObject<MobEffect> SAND_BLIND = EFFECTS.register("sand_blind", SandBlindEffect::new);
    public static final RegistryObject<MobEffect> DEAFEN = EFFECTS.register("deafen", DeafenEffect::new);
    public static final RegistryObject<MobEffect> TRUE_INVISIBILITY = EFFECTS.register("true_invisibility", TrueInvisibilityEffect::new);
    public static final RegistryObject<MobEffect> EROSION_BRAND_MARKER_1 = EFFECTS.register("erosion_brand_marker_1", () -> new ErosionBrandMarkerEffect(0xFFD700));
    public static final RegistryObject<MobEffect> EROSION_BRAND_MARKER_2 = EFFECTS.register("erosion_brand_marker_2", () -> new ErosionBrandMarkerEffect(0xFF8C00));
    public static final RegistryObject<MobEffect> EROSION_BRAND_MARKER_3 = EFFECTS.register("erosion_brand_marker_3", () -> new ErosionBrandMarkerEffect(0xDC143C));

    // ===== 空壳：行为依赖能力/平衡/网络等未迁移系统 =====
    public static final RegistryObject<MobEffect> FOX_FIRE_BURN = shell("fox_fire_burn", () -> new ShellEffect(MobEffectCategory.HARMFUL, 0x3366FF));
    public static final RegistryObject<MobEffect> BLUE_FIRE_RING = shell("blue_fire_ring", () -> new ShellEffect(MobEffectCategory.BENEFICIAL, 0x3366FF));
    public static final RegistryObject<MobEffect> PLAYING_DEAD = shell("playing_dead", () -> new ShellEffect(MobEffectCategory.BENEFICIAL, 0x586e7c));
    public static final RegistryObject<MobEffect> PRE_INVISIBILITY = shell("pre_invisibility", () -> new ShellEffect(MobEffectCategory.BENEFICIAL, 0x101010).modifier(Attributes.MOVEMENT_SPEED, "12db6328-9844-4e20-9118-202758169972", -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistryObject<MobEffect> FROST_FREEZE = shell("frost_freeze", () -> new ShellEffect(MobEffectCategory.HARMFUL, 0x7DD3FC));
    public static final RegistryObject<MobEffect> FROST_FALL = shell("frost_fall", () -> new ShellEffect(MobEffectCategory.HARMFUL, 0xADD8E6));
    public static final RegistryObject<MobEffect> PURIFIED = shell("purified", () -> new ShellEffect(MobEffectCategory.NEUTRAL, 0x99DDFF));
    public static final RegistryObject<MobEffect> TIDAL_SLOW = shell("tidal_slow", () -> new ShellEffect(MobEffectCategory.HARMFUL, 0x33CCFF));
    public static final RegistryObject<MobEffect> CURSE_MARK = shell("curse_mark", () -> new ShellEffect(MobEffectCategory.HARMFUL, 0x7B2FBE));
    public static final RegistryObject<MobEffect> FROST_SHATTER = shell("frost_shatter", () -> new ShellEffect(MobEffectCategory.HARMFUL, 0x9FD8EF));
    public static final RegistryObject<MobEffect> SHEEP_FORM = shell("sheep_form", () -> new ShellEffect(MobEffectCategory.HARMFUL, 0xE8D8C0).modifier(Attributes.ATTACK_DAMAGE, "A51E7C2D-9B34-4F5E-8D6A-3C0B7E1F9A24", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistryObject<MobEffect> SPIDER_WEB_BOUND = shell("spider_web_bound", () -> new ShellEffect(MobEffectCategory.HARMFUL, 0xBFC4CC).modifier(Attributes.MOVEMENT_SPEED, "6C9E2A1F-8B3D-4A7E-9F21-3D5C7A0B1E44", -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));

    // ===== 异常反应标记（anomaly_*），行为由异常系统驱动 =====
    public static final RegistryObject<MobEffect> ANOMALY_LUNAR_ECLIPSE = EFFECTS.register("anomaly_lunar_eclipse", () -> new AnomalyMarkerEffect(0xDBD7A7));
    public static final RegistryObject<MobEffect> ANOMALY_CURSE_WEAKNESS = EFFECTS.register("anomaly_curse_weakness", () -> new AnomalyMarkerEffect(0x8B56AF));
    public static final RegistryObject<MobEffect> ANOMALY_SUMMON_MARK = EFFECTS.register("anomaly_summon_mark", () -> new AnomalyMarkerEffect(0x79CBA7));
    public static final RegistryObject<MobEffect> ANOMALY_VOID_EROSION = EFFECTS.register("anomaly_void_erosion", () -> new AnomalyMarkerEffect(0x6961A5));
    public static final RegistryObject<MobEffect> ANOMALY_SPACE_BIND = EFFECTS.register("anomaly_space_bind", () -> new AnomalyMarkerEffect(0xAFDCEC));
    public static final RegistryObject<MobEffect> ANOMALY_FROST_SLOW = EFFECTS.register("anomaly_frost_slow", () -> new AnomalyMarkerEffect(0x72BDCF));
    public static final RegistryObject<MobEffect> ANOMALY_NO_SPRINT = EFFECTS.register("anomaly_no_sprint", () -> new AnomalyMarkerEffect(0xAFDCEC));
    public static final RegistryObject<MobEffect> ANOMALY_FLAME_SPIRIT = EFFECTS.register("anomaly_flame_spirit", () -> new AnomalyMarkerEffect(0xE5A25B));
    public static final RegistryObject<MobEffect> ANOMALY_FROST_HUNT = EFFECTS.register("anomaly_frost_hunt", () -> new AnomalyMarkerEffect(0x8FCEE5));
    public static final RegistryObject<MobEffect> ANOMALY_SPIRIT_MOON = EFFECTS.register("anomaly_spirit_moon", () -> new AnomalyMarkerEffect(0xB4E9D5));
    public static final RegistryObject<MobEffect> ANOMALY_SUMMON_WEAKNESS = EFFECTS.register("anomaly_summon_weakness", () -> new AnomalyMarkerEffect(0xA48FBC));
    public static final RegistryObject<MobEffect> ANOMALY_FIRE_ZONE = EFFECTS.register("anomaly_fire_zone", () -> new AnomalyMarkerEffect(0xE57F46));

    // ===== 月尘缓解（瞬时，作用于崩解度，待新魔法运行时迁移） =====
    public static final RegistryObject<MobEffect> MOON_DUST_RELIEF = shell("moon_dust_relief", () -> new ShellEffect.Instant(MobEffectCategory.BENEFICIAL, MOON_DUST_COLOR));
    public static final RegistryObject<MobEffect> MOON_DUST_TUBE_RELIEF = shell("moon_dust_tube_relief", () -> new ShellEffect.Instant(MobEffectCategory.BENEFICIAL, MOON_DUST_COLOR));
    public static final RegistryObject<Potion> MOON_DUST_TUBE = POTIONS.register("moon_dust_tube", () ->
            new Potion(new MobEffectInstance(MOON_DUST_TUBE_RELIEF.get(), 1)));

    private static RegistryObject<MobEffect> shell(String id, Supplier<MobEffect> factory) {
        return EFFECTS.register(id, factory);
    }


    public static void registerBrewing() {
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.UNIVERSAL_ENERGY_POTION.get()),
                Ingredient.of(Items.GUNPOWDER), new ItemStack(AddonItems.UNIVERSAL_ENERGY_POTION_SPLASH.get())));
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.UNIVERSAL_ENERGY_POTION_SPLASH.get()),
                Ingredient.of(Items.DRAGON_BREATH), new ItemStack(AddonItems.UNIVERSAL_ENERGY_POTION_LINGERING.get())));
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.INFINITE_ENERGY_POTION.get()),
                Ingredient.of(Items.GUNPOWDER), new ItemStack(AddonItems.INFINITE_ENERGY_POTION_SPLASH.get())));
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.INFINITE_ENERGY_POTION_SPLASH.get()),
                Ingredient.of(Items.DRAGON_BREATH), new ItemStack(AddonItems.INFINITE_ENERGY_POTION_LINGERING.get())));
    }
    private AddonEffects() {}
}
