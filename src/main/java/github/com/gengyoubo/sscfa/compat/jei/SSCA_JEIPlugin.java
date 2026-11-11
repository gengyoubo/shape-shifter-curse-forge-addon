package github.com.gengyoubo.sscfa.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import github.com.gengyoubo.sscfa.block.EnergyBottlerBlockEntity;
import github.com.gengyoubo.sscfa.client.screen.SlottedFormationScreen;
import github.com.gengyoubo.sscfa.compat.recipeviewer.SscViewerRecipes;
import github.com.gengyoubo.sscfa.compat.recipeviewer.SscViewerRecipes.Crafting;
import github.com.gengyoubo.sscfa.compat.recipeviewer.SscViewerRecipes.Process;
import github.com.gengyoubo.sscfa.init.AddonBlocks;
import github.com.gengyoubo.sscfa.init.AddonItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

@JeiPlugin
public final class SSCA_JEIPlugin implements IModPlugin {
    public static final RecipeType<Crafting> SPECIAL_CRAFTING =
            RecipeType.create("ssc_addon", "special_crafting", Crafting.class);
    public static final RecipeType<Process> PROCESSING =
            RecipeType.create("ssc_addon", "processing", Process.class);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("ssc_addon", "jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(AddonItems.TEST_TUBE_POTION.get(), new ISubtypeInterpreter<ItemStack>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                return SscViewerRecipes.subtypeData(stack);
            }
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return SscViewerRecipes.subtypeData(stack).toString();
            }
        });
        registration.registerSubtypeInterpreter(AddonItems.MOON_DUST_VIAL.get(), new ISubtypeInterpreter<ItemStack>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                return SscViewerRecipes.subtypeData(stack);
            }
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return SscViewerRecipes.subtypeData(stack).toString();
            }
        });
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(SscViewerRecipes.viewerStacks());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CraftingCategory(gui), new ProcessingCategory(gui));
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new SscClickTransferHandler<>(CraftingMenu.class, registration.getTransferHelper()), SPECIAL_CRAFTING);
        registration.addRecipeTransferHandler(new SscClickTransferHandler<>(InventoryMenu.class, registration.getTransferHelper()), SPECIAL_CRAFTING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), SPECIAL_CRAFTING);
        registration.addRecipeCatalyst(new ItemStack(AddonBlocks.ENERGY_BOTTLER.get()), PROCESSING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(SlottedFormationScreen.class, screen -> new mezz.jei.api.gui.handlers.IGuiProperties() {
            @Override public Class<? extends Screen> getScreenClass() { return SlottedFormationScreen.class; }
            @Override public int getGuiLeft() { return 0; }
            @Override public int getGuiTop() { return 0; }
            @Override public int getGuiXSize() { return screen.width; }
            @Override public int getGuiYSize() { return screen.height; }
            @Override public int getScreenWidth() { return screen.width; }
            @Override public int getScreenHeight() { return screen.height; }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var world = Minecraft.getInstance().level;
        registration.addRecipes(SPECIAL_CRAFTING, world == null ? List.of() : SscViewerRecipes.crafting(world.getRecipeManager()));
        registration.addRecipes(PROCESSING, SscViewerRecipes.processes());
        var factory = registration.getJeiHelpers().getVanillaRecipeFactory();
        registration.addRecipes(RecipeTypes.BREWING, SscViewerRecipes.brewing().stream().map(r ->
                factory.createBrewingRecipe(List.of(r.input()), r.ingredient(), r.output(), r.id())).toList());
        for (var info : SscViewerRecipes.information()) registration.addItemStackInfo(info.stacks(), info.description());
    }

    private static void description(GuiGraphics graphics, Component text, int y, int width) {
        var font = Minecraft.getInstance().font;
        for (var line : font.split(text, width)) {
            graphics.drawString(font, line, 1, y, 0xFF555555, false);
            y += 10;
        }
    }

    public static final class CraftingCategory implements IRecipeCategory<Crafting> {
        private final IDrawable background, icon, slotBackground;

        public CraftingCategory(IGuiHelper gui) {
            background = gui.createBlankDrawable(144, 166);
            icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.CRAFTING_TABLE));
            slotBackground = gui.getSlotDrawable();
        }

        @Override public RecipeType<Crafting> getRecipeType() { return SPECIAL_CRAFTING; }
        @Override public Component getTitle() { return Component.translatable("gui.ssc_addon.category.special_crafting"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public ResourceLocation getRegistryName(Crafting recipe) { return recipe.id(); }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Crafting recipe, IFocusGroup focuses) {
            for (int i = 0; i < recipe.grid().size(); i++) {
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, 1 + i % 3 * 18, 1 + i / 3 * 18);
                slot.setBackground(slotBackground, -1, -1);
                slot.addItemStacks(recipe.grid().get(i));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 19).setBackground(slotBackground, -1, -1).addItemStack(recipe.output());
        }

        @Override
        public void draw(Crafting recipe, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
            graphics.blit(new ResourceLocation("minecraft", "textures/gui/container/crafting_table.png"),
                    60, 24, 90, 40, 22, 15, 256, 256);
            description(graphics, recipe.description(), 60, 140);
        }
    }

    public static final class ProcessingCategory implements IRecipeCategory<Process> {
        private final IDrawable background, icon, slotBackground;

        public ProcessingCategory(IGuiHelper gui) {
            background = gui.createBlankDrawable(160, 140);
            icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(AddonBlocks.ENERGY_BOTTLER.get()));
            slotBackground = gui.getSlotDrawable();
        }

        @Override public RecipeType<Process> getRecipeType() { return PROCESSING; }
        @Override public Component getTitle() { return Component.translatable("gui.ssc_addon.category.processing"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public ResourceLocation getRegistryName(Process recipe) { return recipe.id(); }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Process recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 1, 1).setBackground(slotBackground, -1, -1).addItemStack(recipe.workstation());
            builder.addSlot(RecipeIngredientRole.INPUT, 37, 1).setBackground(slotBackground, -1, -1).addItemStacks(recipe.inputs());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 111, 1).setBackground(slotBackground, -1, -1).addItemStack(recipe.output());
        }

        @Override
        public void draw(Process recipe, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
            graphics.blit(new ResourceLocation("minecraft", "textures/gui/container/crafting_table.png"),
                    72, 2, 90, 40, 22, 15, 256, 256);
            description(graphics, recipe.description(), 28, 156);
        }
    }
}
