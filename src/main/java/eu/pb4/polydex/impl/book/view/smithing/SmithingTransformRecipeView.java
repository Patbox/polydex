package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.PolydexUtils;
import eu.pb4.polydex.mixin.SmithingTransformRecipeAccessor;
import eu.pb4.polydex.mixin.SmithingTransformRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.recipe.SmithingTransformRecipe;

public class SmithingTransformRecipeView extends AbstractSmithingRecipeView<SmithingTransformRecipe> {
    @Override
    protected ItemStack[] getTemplate(SmithingTransformRecipe recipe) {
        return PolydexUtils.readIngredient(cast(recipe).getTemplate());
    }

    @Override
    protected ItemStack[] getAddition(SmithingTransformRecipe recipe) {
        return PolydexUtils.readIngredient(cast(recipe).getAddition());
    }

    @Override
    protected ItemStack[] getBase(SmithingTransformRecipe recipe) {
        return PolydexUtils.readIngredient(cast(recipe).getBase());
    }

    private SmithingTransformRecipeAccessor cast(SmithingTransformRecipe recipe) {
        return (SmithingTransformRecipeAccessor) recipe;
    }
}
