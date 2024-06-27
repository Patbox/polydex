package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

public final class ShapedCraftingRecipePage extends AbstractCraftingRecipePage<ShapedRecipe> {
    public ShapedCraftingRecipePage(RecipeEntry<ShapedRecipe> recipe) {
        super(recipe);
    }

    protected Ingredient getStacksAt(ShapedRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        if (x < recipe.getWidth() && y < recipe.getHeight()) {
            return recipe.getIngredients().get(x + (recipe.getWidth() * y));
        }
        return Ingredient.EMPTY;
    };
}
