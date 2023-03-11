package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.PolydexUtils;
import eu.pb4.polydex.impl.book.view.crafting.AbstractCraftingRecipeView;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ShapedCraftingRecipeView extends AbstractCraftingRecipeView<ShapedRecipe> {
    protected ItemStack[] getStacksAt(ShapedRecipe recipe, ServerPlayerEntity player, int x, int y) {
        if (x < recipe.getWidth() && y < recipe.getHeight()) {
            return PolydexUtils.readIngredient(recipe.getIngredients().get(x + (recipe.getWidth() * y)));
        }
        return new ItemStack[0];
    };
}
