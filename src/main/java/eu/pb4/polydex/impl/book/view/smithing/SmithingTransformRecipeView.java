package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.mixin.SmithingTransformRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class SmithingTransformRecipeView extends AbstractSmithingRecipeView<SmithingTransformRecipe> {
    public SmithingTransformRecipeView(RecipeEntry<SmithingTransformRecipe> recipe) {
        super(recipe);
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((SmithingTransformRecipeAccessor) this.recipe).getResult().copy();
    }

    @Override
    protected Ingredient getTemplate() {
        return recipe.template().orElse(null);
    }

    @Override
    protected Ingredient getAddition() {
        return recipe.addition().orElse(null);
    }

    @Override
    protected Ingredient getBase() {
        return recipe.base().orElse(null);
    }

    @Override
    public boolean syncWithClient(ServerPlayerEntity player) {
        return false;
    }
}
