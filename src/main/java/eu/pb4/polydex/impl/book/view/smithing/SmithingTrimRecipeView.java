package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.PolydexUtils;
import eu.pb4.polydex.mixin.SmithingTrimRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmithingTrimRecipe;

public class SmithingTrimRecipeView extends AbstractSmithingRecipeView<SmithingTrimRecipe> {
    @Override
    protected ItemStack[] getTemplate(SmithingTrimRecipe recipe) {
        return PolydexUtils.readIngredient(cast(recipe).getTemplate());
    }

    @Override
    protected ItemStack[] getAddition(SmithingTrimRecipe recipe) {
        return PolydexUtils.readIngredient(cast(recipe).getAddition());
    }

    @Override
    protected ItemStack[] getBase(SmithingTrimRecipe recipe) {
        return PolydexUtils.readIngredient(cast(recipe).getBase());
    }

    private SmithingTrimRecipeAccessor cast(SmithingTrimRecipe recipe) {
        return (SmithingTrimRecipeAccessor) recipe;
    }
}
