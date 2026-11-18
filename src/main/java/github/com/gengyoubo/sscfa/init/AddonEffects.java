package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.effect.*;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

    public static final RegistryObject<MobEffect> FOX_FIRE_BURN = EFFECTS.register("fox_fire_burn", FoxFireBurnEffect::new);
    public static final RegistryObject<MobEffect> BLUE_FIRE_RING = EFFECTS.register("blue_fire_ring", BlueFireRingEffect::new);
    public static final RegistryObject<MobEffect> PLAYING_DEAD = EFFECTS.register("playing_dead", PlayingDeadEffect::new);
    public static final RegistryObject<MobEffect> PRE_INVISIBILITY = EFFECTS.register("pre_invisibility", PreInvisibilityEffect::new);
    public static final RegistryObject<MobEffect> FROST_FREEZE = EFFECTS.register("frost_freeze", FrostFreezeEffect::new);
    public static final RegistryObject<MobEffect> FROST_FALL = EFFECTS.register("frost_fall", FrostFallEffect::new);
    public static final RegistryObject<MobEffect> PURIFIED = EFFECTS.register("purified", PurifiedEffect::new);
    public static final RegistryObject<MobEffect> TIDAL_SLOW = EFFECTS.register("tidal_slow", TidalSlowEffect::new);
    public static final RegistryObject<MobEffect> CURSE_MARK = EFFECTS.register("curse_mark", CurseMarkEffect::new);
    public static final RegistryObject<MobEffect> FROST_SHATTER = EFFECTS.register("frost_shatter", FrostShatterEffect::new);
    public static final RegistryObject<MobEffect> SHEEP_FORM = EFFECTS.register("sheep_form", SheepFormEffect::new);
    public static final RegistryObject<MobEffect> SPIDER_WEB_BOUND = EFFECTS.register("spider_web_bound", SpiderWebBoundEffect::new);

    // ===== 月尘缓解（瞬时，作用于崩解度，待新魔法运行时迁移） =====
    public static final RegistryObject<MobEffect> MOON_DUST_RELIEF = EFFECTS.register("moon_dust_relief",
            () -> github.com.gengyoubo.sscfa.item.MoonDustReliefItem.RELIEF);
    public static final RegistryObject<MobEffect> MOON_DUST_TUBE_RELIEF = EFFECTS.register("moon_dust_tube_relief", () -> new MobEffect(MobEffectCategory.BENEFICIAL, MOON_DUST_COLOR) {
        @Override public boolean isInstantenous() { return true; }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return false; }
        @Override public void applyInstantenousEffect(net.minecraft.world.entity.Entity source, net.minecraft.world.entity.Entity attacker,
                                                      net.minecraft.world.entity.LivingEntity target, int amplifier, double proximity) {
            // Each test tube carries one third of a bottle; splash proximity scales it like the bottle relief.
            if (target instanceof net.minecraft.server.level.ServerPlayer player && Double.isFinite(proximity) && proximity > 0)
                github.com.gengyoubo.sscfa.neomagic.NeoRuntime.relieve(player,
                        github.com.gengyoubo.sscfa.item.MoonDustReliefItem.bottle().relief()
                                / github.com.gengyoubo.sscfa.item.PotionTestTubes.PORTIONS * Math.min(1, proximity));
        }
    });
    public static final RegistryObject<Potion> MOON_DUST_TUBE = POTIONS.register("moon_dust_tube", () ->
            new Potion(new MobEffectInstance(MOON_DUST_TUBE_RELIEF.get(), 1)));



    public static void registerBrewing() {
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.WITHER_POTION.get()),
                Ingredient.of(Items.GUNPOWDER), new ItemStack(AddonItems.WITHER_POTION_SPLASH.get())));
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.WITHER_POTION_SPLASH.get()),
                Ingredient.of(Items.DRAGON_BREATH), new ItemStack(AddonItems.WITHER_POTION_LINGERING.get())));
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
