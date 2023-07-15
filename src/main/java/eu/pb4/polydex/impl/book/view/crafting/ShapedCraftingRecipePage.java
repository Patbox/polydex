package eu.pb4.polydex.impl.book.view.crafting;

import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapedRecipe;

public final class ShapedCraftingRecipePage extends AbstractCraftingRecipePage<ShapedRecipe> {
    public ShapedCraftingRecipePage(ShapedRecipe recipe) {
        super(recipe);
    }

    protected Ingredient getStacksAt(ShapedRecipe recipe, int x, int y) {
        if (x < recipe.getWidth() && y < recipe.getHeight()) {
            return recipe.getIngredients().get(x + (recipe.getWidth() * y));
        }
        return Ingredient.EMPTY;
    };
}
