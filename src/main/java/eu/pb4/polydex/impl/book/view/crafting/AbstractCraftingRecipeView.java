package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.api.PolydexUiElements;
import eu.pb4.polydex.api.PolydexUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

public abstract class AbstractCraftingRecipeView<T extends CraftingRecipe> implements ItemPageView<T> {
    private static GuiElement CRAFTING_TABLE = PolydexUiElements.CRAFTING_TABLE_RECIPE_ICON;
    private static GuiElement CRAFTING = PolydexUiElements.CRAFTING_RECIPE_ICON;

    @Override
    public GuiElement getIcon(ItemEntry entry, T recipe, ServerPlayerEntity player, Runnable returnCallback) {
        return recipe.fits(2, 2) ? CRAFTING : CRAFTING_TABLE;
    }

    @Override
    public void renderLayer(ItemEntry entry, T recipe, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        for (int i = 0; i < 9; i++) {
            var x = i % 3;
            var y = i / 3;

            layer.setSlot(x + (9 * y) + 11, PolydexUtils.getIngredientDisplay(getStacksAt(recipe, player, x, y)));
        }


        layer.setSlot(24, new GuiElement(recipe.getOutput(player.server.getRegistryManager()), GuiElement.EMPTY_CALLBACK));
    }
    protected abstract ItemStack[] getStacksAt(T recipe, ServerPlayerEntity player, int x, int y);
}
