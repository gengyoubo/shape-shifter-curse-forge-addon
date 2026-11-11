package github.com.gengyoubo.sscfa.compat.recipeviewer;

import github.com.gengyoubo.sscfa.compat.recipeviewer.SscTransferPlan.Outcome;
import github.com.gengyoubo.sscfa.compat.recipeviewer.SscViewerRecipes.Crafting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/** Shared client adapter for JEI. The server processes ordinary inventory clicks. */
public final class SscGridClickTransfer {
    private SscGridClickTransfer() {}

    public static boolean supports(AbstractContainerMenu menu, Crafting recipe) {
        return menu instanceof CraftingMenu || (recipe.shapeless() && menu instanceof InventoryMenu);
    }

    public static Outcome transfer(Minecraft client, Crafting recipe, boolean checkOnly) {
        if (client.player == null || client.gameMode == null) return Outcome.FAILED;
        AbstractContainerMenu menu = client.player.containerMenu;
        if (!supports(menu, recipe)) return Outcome.FAILED;
        int width = menu instanceof CraftingMenu ? 3 : 2;
        var grid = new ArrayList<ItemStack>();
        var inventory = new ArrayList<ItemStack>();
        for (int i = 0; i < width * width; i++) grid.add(menu.getSlot(1 + i).getItem());
        for (int i = 0; i < 36; i++) inventory.add(client.player.getInventory().getItem(i));
        var plan = SscTransferPlan.create(recipe.gridFor(width), grid, inventory, menu.getCarried());
        if (checkOnly || plan.outcome() != Outcome.SUCCESS) return plan.outcome();
        for (var click : plan.clicks()) {
            int slot;
            if (click.slot() < grid.size()) slot = 1 + click.slot();
            else {
                int inv = click.slot() - grid.size();
                slot = inv < 9 ? (width == 3 ? 37 : 36) + inv : (width == 3 ? 1 : 0) + inv;
            }
            client.gameMode.handleInventoryMouseClick(menu.containerId, slot, click.button(), ClickType.PICKUP, client.player);
        }
        return menu.getCarried().isEmpty() ? Outcome.SUCCESS : Outcome.FAILED;
    }
}
