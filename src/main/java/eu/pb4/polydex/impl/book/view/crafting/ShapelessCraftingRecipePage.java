package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

public final class ShapelessCraftingRecipePage extends AbstractCraftingRecipePage<ShapelessRecipe> {
    public ShapelessCraftingRecipePage(RecipeEntry<ShapelessRecipe> recipe) {
        super(recipe);
    }

    protected Ingredient getStacksAt(ShapelessRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        var list = recipe.getIngredients();
        var i = x + y * 3;
        if (i < list.size()) {
            return recipe.getIngredients().get(i);
        }
        return Ingredient.EMPTY;
    };
}
