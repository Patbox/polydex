package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.recipe.*;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;


public abstract class AbstractSmithingRecipeView<T extends SmithingRecipe> extends SimpleRecipePolydexPage<T> {
    private final List<PolydexIngredient<?>> ingrendients;

    public AbstractSmithingRecipeView(T recipe) {
        super(recipe);
        this.ingrendients = List.of(PolydexIngredient.of(getBase()), PolydexIngredient.of(getTemplate()), PolydexIngredient.of(getAddition()));
    }

    @Override
    public ItemStack getIcon(ServerPlayerEntity player) {
        return PageIcons.SMITING_RECIPE_ICON;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(2, 2,this.getTemplate());
        builder.setIngredient(3, 2, this.getBaseItem(entry));
        builder.setIngredient(4, 2, this.getAddition());
        builder.setOutput(6, 2, this.getOutput(entry, player));
    }

    protected ItemStack[] getOutput(PolydexEntry entry, ServerPlayerEntity player) {
        return new ItemStack[] { recipe.getOutput(player.server.getRegistryManager()) };
    }

    @Override
    public List<PolydexIngredient<?>> getIngredients() {
        return this.ingrendients;
    }

    protected Ingredient getBaseItem(PolydexEntry entry) {
        return getBase();
    }

    protected abstract Ingredient getTemplate();
    protected abstract Ingredient getAddition();
    protected abstract Ingredient getBase();
}
