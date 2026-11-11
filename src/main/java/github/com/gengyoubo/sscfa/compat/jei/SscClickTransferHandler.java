package github.com.gengyoubo.sscfa.compat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import github.com.gengyoubo.sscfa.compat.recipeviewer.SscGridClickTransfer;
import github.com.gengyoubo.sscfa.compat.recipeviewer.SscTransferPlan.Outcome;
import github.com.gengyoubo.sscfa.compat.recipeviewer.SscViewerRecipes.Crafting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Optional;

public final class SscClickTransferHandler<T extends AbstractContainerMenu> implements IRecipeTransferHandler<T, Crafting> {
    private final Class<T> container;
    private final IRecipeTransferHandlerHelper helper;

    public SscClickTransferHandler(Class<T> container, IRecipeTransferHandlerHelper helper) {
        this.container = container;
        this.helper = helper;
    }

    @Override public Class<? extends T> getContainerClass() { return container; }
    @Override public Optional<MenuType<T>> getMenuType() { return Optional.empty(); }
    @Override public RecipeType<Crafting> getRecipeType() { return SSCA_JEIPlugin.SPECIAL_CRAFTING; }

    @Override
    public IRecipeTransferError transferRecipe(T menu, Crafting recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (!SscGridClickTransfer.supports(menu, recipe)) return helper.createInternalError();
        Outcome outcome = SscGridClickTransfer.transfer(Minecraft.getInstance(), recipe, !doTransfer);
        if (outcome == Outcome.SUCCESS) return null;
        String key = switch (outcome) {
            case NOT_ENOUGH -> "not_enough_materials";
            case NO_ROOM -> "no_room";
            case CURSOR_NOT_EMPTY -> "cursor_not_empty";
            default -> "failed";
        };
        return helper.createUserErrorWithTooltip(Component.translatable("error.ssc_addon.transfer." + key));
    }
}
