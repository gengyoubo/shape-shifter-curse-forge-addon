package github.com.gengyoubo.sscfa.compat.recipeviewer;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.block.EnergyBottlerBlockEntity;
import github.com.gengyoubo.sscfa.compat.RegCustomItem;
import github.com.gengyoubo.sscfa.init.AddonBlocks;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.item.PortableFridgeItem;
import github.com.gengyoubo.sscfa.item.PortableMoisturizerItem;
import github.com.gengyoubo.sscfa.item.PotionTestTubes;
import github.com.gengyoubo.sscfa.item.SnowballLauncherItem;
import github.com.gengyoubo.sscfa.recipe.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.onixary.shapeShifterCurseForge.registry.ModPotions;

import java.util.ArrayList;
import java.util.List;

/** Viewer-neutral examples. Only recipes present in the current recipe manager are advertised. */
public final class SscViewerRecipes {
    private SscViewerRecipes() {}

    public record Crafting(ResourceLocation id, List<List<ItemStack>> grid, ItemStack output,
                           boolean shapeless, boolean dynamic, String note) {
        public int width() { return shapeless ? 2 : 3; }
        public int height() { return shapeless ? Math.max(1, (int)(grid.stream().filter(s -> !s.isEmpty()).count() + 1) / 2) : 3; }
        public List<List<ItemStack>> gridFor(int width) {
            if (!shapeless) return grid;
            var result = emptyGrid(width * width);
            int index = 0;
            for (var alternatives : grid) if (!alternatives.isEmpty()) result.set(index++, alternatives);
            return result;
        }
        public Component description() { return Component.translatable("recipe.ssc_addon." + note); }
    }

    public record Brewing(ResourceLocation id, ItemStack input, ItemStack ingredient, ItemStack output) {}
    public record Process(ResourceLocation id, ItemStack workstation, List<ItemStack> inputs,
                          ItemStack output, Component description) {}
    public record Info(List<ItemStack> stacks, String key) {
        public Component description() { return Component.translatable("recipe.ssc_addon." + key); }
    }

    public static List<Crafting> crafting(RecipeManager manager) {
        return manager.getRecipes().stream().flatMap(recipe -> examples(recipe).stream()).toList();
    }

    public static List<Crafting> examples(Recipe<?> recipe) {
        List<Crafting> result = new ArrayList<>();
        if (recipe instanceof TestTubeFillingRecipe) {
            for (ItemStack bottle : fillingBottles()) {
                var grid = emptyGrid(9);
                grid.set(0, List.of(bottle));
                for (int i = 1; i <= 3; i++) grid.set(i, List.of(new ItemStack(PotionTestTubes.EMPTY)));
                var potionId = BuiltInRegistries.POTION.getKey(PotionUtils.getPotion(bottle));
                result.add(example(recipe, bottle.is(AddonItems.MOON_DUST_BOTTLE.get()) ? "moon_dust" : potionId.getNamespace() + "/" + potionId.getPath(),
                        grid, PotionTestTubes.divide(bottle), true, true, "test_tube_filling"));
            }
        } else if (recipe instanceof VenomGlandRecipe) {
            var grid = emptyGrid(9);
            for (int i = 0; i < 9; i++) grid.set(i, List.of(new ItemStack(Items.SPIDER_EYE)));
            grid.set(4, potions(Potions.POISON, Potions.LONG_POISON, Potions.STRONG_POISON));
            result.add(example(recipe, "", grid, new ItemStack(SscAddon.VENOM_GLAND), false, false, "venom"));
        } else if (recipe instanceof InfiniteEnergyPotionRecipe) {
            var grid = emptyGrid(9);
            grid.set(1, List.of(new ItemStack(SscAddon.SP_UPGRADE_THING)));
            grid.set(3, List.of(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));
            grid.set(4, potions(ModPotions.FEED.get()));
            grid.set(5, List.of(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));
            result.add(example(recipe, "", grid, new ItemStack(SscAddon.INFINITE_ENERGY_POTION), false, false, "infinite"));
        } else if (recipe instanceof SpUpgradeRecipe) {
            var grid = emptyGrid(9);
            for (int i : new int[]{0, 2, 6, 8}) grid.set(i, List.of(new ItemStack(Items.GOLD_INGOT)));
            grid.set(4, List.of(new ItemStack(RegCustomItem.MORPHSCALE_CORE)));
            int[] sides = {1, 3, 5, 7};
            Item[] materials = {Items.EMERALD, Items.REDSTONE, RegCustomItem.MOONDUST_CRYSTAL_SHARD, Items.NETHERITE_INGOT};
            for (int i = 0; i < 4; i++) grid.set(sides[i], List.of(new ItemStack(materials[i])));
            result.add(example(recipe, "", grid, new ItemStack(SscAddon.SP_UPGRADE_THING), false, false, "moon_ring"));
        } else if (recipe instanceof RefillMoisturizerRecipe) {
            for (int level = 1; level <= 3; level++) {
                ItemStack input = moisturizer(level);
                ItemStack output = input.copy();
                PortableMoisturizerItem.setFullCharge(output);
                result.add(example(recipe, "level_" + level, pair(input, new ItemStack(Items.AXOLOTL_BUCKET)), output, true, true, "refill"));
            }
        } else if (recipe instanceof UpgradeMoisturizerRecipe) {
            for (int level = 1; level < 3; level++) {
                var grid = emptyGrid(9);
                for (int i : new int[]{0, 2, 6, 8}) grid.set(i, List.of(new ItemStack(level == 1 ? Items.WET_SPONGE : Items.HEART_OF_THE_SEA)));
                for (int i : new int[]{1, 3, 5, 7}) grid.set(i, List.of(new ItemStack(level == 1 ? Items.GOLD_INGOT : Items.DIAMOND)));
                grid.set(4, List.of(moisturizer(level)));
                result.add(example(recipe, "level_" + level, grid, moisturizer(level + 1), false, true, "upgrade_moisturizer"));
            }
        } else if (recipe instanceof ReloadSnowballLauncherRecipe) {
            for (Item ammo : List.of(Items.SNOWBALL, Items.SNOW, Items.SNOW_BLOCK)) {
                ItemStack input = new ItemStack(SscAddon.SNOWBALL_LAUNCHER);
                SnowballLauncherItem.setAmmo(input, 0);
                ItemStack output = input.copy();
                SnowballLauncherItem.setAmmo(output, ammo == Items.SNOW_BLOCK ? 8 : 1);
                result.add(example(recipe, BuiltInRegistries.ITEM.getKey(ammo).getPath(), pair(input, new ItemStack(ammo)), output, true, true, "reload"));
            }
        } else if (recipe instanceof BlizzardTankRechargeRecipe) {
            ItemStack input = new ItemStack(SscAddon.PORTABLE_FRIDGE);
            PortableFridgeItem.setCharge(input, 0);
            ItemStack output = input.copy();
            PortableFridgeItem.setCharge(output, 8);
            result.add(example(recipe, "", pair(input, new ItemStack(Items.SNOW_BLOCK)), output, true, true, "recharge"));
        }
        return result;
    }

    private static Crafting example(Recipe<?> recipe, String variant, List<List<ItemStack>> grid,
                                    ItemStack output, boolean shapeless, boolean dynamic, String note) {
        ResourceLocation source = recipe.getId();
        ResourceLocation id = variant.isEmpty() ? source
                : new ResourceLocation(source.getNamespace(), source.getPath() + "/" + variant);
        return new Crafting(id, grid, output, shapeless, dynamic, note);
    }

    public static ItemStack moisturizer(int level) {
        ItemStack stack = new ItemStack(SscAddon.PORTABLE_MOISTURIZER);
        PortableMoisturizerItem.setLevel(stack, level);
        PortableMoisturizerItem.setCharge(stack, 0);
        return stack;
    }

    public static List<ItemStack> potions(Potion... potions) {
        List<ItemStack> result = new ArrayList<>();
        for (Potion potion : potions) for (Item bottle : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION))
            result.add(PotionUtils.setPotion(new ItemStack(bottle), potion));
        return result;
    }

    private static List<List<ItemStack>> pair(ItemStack first, ItemStack second) {
        var grid = emptyGrid(9);
        grid.set(0, List.of(first)); grid.set(1, List.of(second));
        return grid;
    }

    public static List<List<ItemStack>> emptyGrid(int size) {
        var grid = new ArrayList<List<ItemStack>>(size);
        for (int i = 0; i < size; i++) grid.add(List.of());
        return grid;
    }

    public static List<ItemStack> fillingBottles() {
        List<ItemStack> result = new ArrayList<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            ItemStack bottle = PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
            if (PotionTestTubes.isBottle(bottle)) result.add(bottle);
        }
        result.add(new ItemStack(AddonItems.MOON_DUST_BOTTLE.get()));
        return result;
    }

    public static List<ItemStack> viewerStacks() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack bottle : fillingBottles()) {
            ItemStack tube = PotionTestTubes.divide(bottle);
            if (!tube.isEmpty()) result.add(tube);
        }
        return result;
    }

    public static Object subtypeData(ItemStack stack) {
        if (stack.is(AddonItems.MOON_DUST_BOTTLE.get())) return "moon_dust";
        if (!(stack.getItem() instanceof PotionTestTubes.FilledTube)) return "";
        StringBuilder builder = new StringBuilder(BuiltInRegistries.POTION.getKey(PotionUtils.getPotion(stack)).toString());
        for (var effect : PotionUtils.getMobEffects(stack)) {
            builder.append('|').append(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()))
                    .append(':').append(effect.getDuration()).append(':').append(effect.getAmplifier());
        }
        return builder.toString();
    }

    /** Same semantic requirements as matches(): names, charges and unrelated custom NBT are retained. */
    public static boolean matches(ItemStack have, ItemStack want) {
        if (have.isEmpty() || want.isEmpty() || !have.is(want.getItem())) return false;
        if (have.is(Items.POTION) || have.is(Items.SPLASH_POTION) || have.is(Items.LINGERING_POTION))
            return PotionUtils.getPotion(have) == PotionUtils.getPotion(want);
        if (have.is(SscAddon.PORTABLE_MOISTURIZER))
            return PortableMoisturizerItem.getLevel(have) == PortableMoisturizerItem.getLevel(want);
        if (have.is(SscAddon.PORTABLE_FRIDGE)) return PortableFridgeItem.getCharge(have) < PortableFridgeItem.MAX_CHARGE;
        return true;
    }

    public static List<Brewing> brewing() {
        return List.of(
            brew("infinite_splash", SscAddon.INFINITE_ENERGY_POTION, Items.GUNPOWDER, SscAddon.INFINITE_ENERGY_POTION_SPLASH),
            brew("infinite_lingering", SscAddon.INFINITE_ENERGY_POTION_SPLASH, Items.DRAGON_BREATH, SscAddon.INFINITE_ENERGY_POTION_LINGERING),
            brew("universal_splash", SscAddon.UNIVERSAL_ENERGY_POTION, Items.GUNPOWDER, SscAddon.UNIVERSAL_ENERGY_POTION_SPLASH),
            brew("universal_lingering", SscAddon.UNIVERSAL_ENERGY_POTION_SPLASH, Items.DRAGON_BREATH, SscAddon.UNIVERSAL_ENERGY_POTION_LINGERING),
            new Brewing(new ResourceLocation("ssc_addon", "moon_dust_bottle"),
                    PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD),
                    new ItemStack(RegCustomItem.UNTREATED_MOONDUST), new ItemStack(AddonItems.MOON_DUST_BOTTLE.get())),
            brew("moon_dust_splash", AddonItems.MOON_DUST_BOTTLE.get(), Items.GUNPOWDER, AddonItems.MOON_DUST_SPLASH.get()));
    }

    private static Brewing brew(String path, Item input, Item ingredient, Item output) {
        return new Brewing(new ResourceLocation("ssc_addon", path), new ItemStack(input), new ItemStack(ingredient), new ItemStack(output));
    }

    public static List<Process> processes() {
        var result = new ArrayList<Process>();
        result.add(new Process(new ResourceLocation("ssc_addon", "energy_bottling"), new ItemStack(AddonBlocks.ENERGY_BOTTLER.get()),
            List.of(new ItemStack(Items.GLASS_BOTTLE)), EnergyBottlerBlockEntity.makeEnergyBottle(),
            Component.translatable("recipe.ssc_addon.bottling", EnergyBottlerBlockEntity.ENERGY_PER_BOTTLE, EnergyBottlerBlockEntity.CRAFT_TIME / 20)));
        return result;
    }

    /** Research descriptions intentionally contain no solved spell-to-rune layouts or world language. */
    public static List<Info> information() {
        return List.of(
            new Info(List.of(new ItemStack(SscAddon.FROST_SPINE_COLLAR)), "loot_frost_spine_collar"),
            new Info(List.of(new ItemStack(SscAddon.ANKH_STONE)), "loot_ankh_stone"),
            new Info(List.of(new ItemStack(SscAddon.MOON_DUST_SPELLBOOK), new ItemStack(AddonBlocks.INFUSION_ALTAR.get())), "infusion"),
            new Info(List.of(new ItemStack(SscAddon.FORMATION), new ItemStack(SscAddon.BLANK_FORMATION_PAPER), new ItemStack(AddonBlocks.SPELL_RESEARCH_TABLE.get())), "formation"),
            new Info(List.of(new ItemStack(SscAddon.MAGIC_SCROLL), new ItemStack(AddonBlocks.SPELL_RESEARCH_TABLE.get())), "scroll_workshop"),
            new Info(List.of(new ItemStack(SscAddon.SPELL_FORMATION)), "rune_research"),
            new Info(List.of(new ItemStack(SscAddon.ANALYZED_SPELL_DIAGRAM)), "analysis"));
    }
}
