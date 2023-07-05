package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.PolydexUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ShapelessCraftingRecipeView extends AbstractCraftingRecipeView<ShapelessRecipe> {
    protected Ingredient getStacksAt(ShapelessRecipe recipe, int x, int y) {
        var list = recipe.getIngredients();
        var i = x + y * 3;
        if (i < list.size()) {
            return recipe.getIngredients().get(i);
        }
        return Ingredient.EMPTY;
    };
}
