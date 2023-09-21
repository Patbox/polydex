package eu.pb4.polydex.impl.book.view.crafting;

import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapelessRecipe;

public final class ShapelessCraftingRecipePage extends AbstractCraftingRecipePage<ShapelessRecipe> {
    public ShapelessCraftingRecipePage(RecipeEntry<ShapelessRecipe> recipe) {
        super(recipe);
    }

    protected Ingredient getStacksAt(ShapelessRecipe recipe, int x, int y) {
        var list = recipe.getIngredients();
        var i = x + y * 3;
        if (i < list.size()) {
            return recipe.getIngredients().get(i);
        }
        return Ingredient.EMPTY;
    };
}
