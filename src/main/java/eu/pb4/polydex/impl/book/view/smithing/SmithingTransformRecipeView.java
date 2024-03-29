package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.mixin.SmithingTransformRecipeAccessor;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.server.network.ServerPlayerEntity;

public class SmithingTransformRecipeView extends AbstractSmithingRecipeView<SmithingTransformRecipe> {
    public SmithingTransformRecipeView(RecipeEntry<SmithingTransformRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected Ingredient getTemplate() {
        return cast(recipe).getTemplate();
    }

    @Override
    protected Ingredient getAddition() {
        return cast(recipe).getAddition();
    }

    @Override
    protected Ingredient getBase() {
        return cast(recipe).getBase();
    }

    private SmithingTransformRecipeAccessor cast(SmithingTransformRecipe recipe) {
        return (SmithingTransformRecipeAccessor) recipe;
    }

    @Override
    public boolean syncWithClient(ServerPlayerEntity player) {
        return false;
    }
}
