package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.mixin.SmithingTransformRecipeAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.jetbrains.annotations.Nullable;

public class SmithingTransformRecipeView extends AbstractSmithingRecipeView<SmithingTransformRecipe> {
    public SmithingTransformRecipeView(RecipeHolder<SmithingTransformRecipe> recipe) {
        super(recipe);
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((SmithingTransformRecipeAccessor) this.recipe).getResult().apply(entry != null && entry.stack().getBacking() instanceof ItemStack stack ? stack : Items.STONE.getDefaultInstance());
    }

    @Override
    protected Ingredient getTemplate() {
        return recipe.templateIngredient().orElse(null);
    }

    @Override
    protected Ingredient getAddition() {
        return recipe.additionIngredient().orElse(null);
    }

    @Override
    protected Ingredient getBase() {
        return recipe.baseIngredient();
    }

    @Override
    public boolean syncWithClient(ServerPlayer player) {
        return false;
    }
}
