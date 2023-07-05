package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.PolydexUtils;
import eu.pb4.polydex.mixin.SmithingTransformRecipeAccessor;
import eu.pb4.polydex.mixin.SmithingTransformRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.recipe.SmithingTransformRecipe;

public class SmithingTransformRecipeView extends AbstractSmithingRecipeView<SmithingTransformRecipe> {
    @Override
    protected Ingredient getTemplate(SmithingTransformRecipe recipe) {
        return cast(recipe).getTemplate();
    }

    @Override
    protected Ingredient getAddition(SmithingTransformRecipe recipe) {
        return cast(recipe).getAddition();
    }

    @Override
    protected Ingredient getBase(SmithingTransformRecipe recipe) {
        return cast(recipe).getBase();
    }

    private SmithingTransformRecipeAccessor cast(SmithingTransformRecipe recipe) {
        return (SmithingTransformRecipeAccessor) recipe;
    }
}
