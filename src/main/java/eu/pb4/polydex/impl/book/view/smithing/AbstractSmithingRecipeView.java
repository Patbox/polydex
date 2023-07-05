package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.api.PageView;
import eu.pb4.polydex.api.recipe.PageBuilder;
import eu.pb4.polydex.api.recipe.PageIcons;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;


public abstract class AbstractSmithingRecipeView<T extends SmithingRecipe> implements PageView<T> {
    @Override
    public ItemStack getIcon(ItemEntry entry, T recipe, ServerPlayerEntity player) {
        return PageIcons.SMITING_RECIPE_ICON;
    }

    @Override
    public void createPage(ItemEntry entry, T recipe, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(2, 2,this.getTemplate(recipe));
        builder.setIngredient(3, 2, this.getBaseItem(entry, recipe));
        builder.setIngredient(4, 2, this.getAddition(recipe));
        builder.setOutput(6, 2, this.getOutput(entry, player, recipe));
    }

    protected ItemStack[] getOutput(ItemEntry entry, ServerPlayerEntity player, T recipe) {
        return new ItemStack[] { recipe.getOutput(player.server.getRegistryManager()) };
    }

    @Override
    public List<Ingredient> getIngredients(T object) {
        return List.of(getBase(object), getTemplate(object), getAddition(object));
    }

    protected Ingredient getBaseItem(ItemEntry entry, T recipe) {
        return getBase(recipe);
    }

    protected abstract Ingredient getTemplate(T recipe);
    protected abstract Ingredient getAddition(T recipe);
    protected abstract Ingredient getBase(T recipe);
}
