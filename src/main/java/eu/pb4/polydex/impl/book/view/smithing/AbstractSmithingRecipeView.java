package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public abstract class AbstractSmithingRecipeView<T extends SmithingRecipe> extends AbstractRecipePolydexPage<T> {
    private final List<PolydexIngredient<?>> ingrendients;

    public AbstractSmithingRecipeView(RecipeEntry<T> recipe) {
        super(recipe);
        this.ingrendients = List.of(PolydexIngredient.of(getBase()), PolydexIngredient.of(getTemplate()), PolydexIngredient.of(getAddition()));
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return InternalPageTextures.SMITHING;
    }

    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return PageIcons.SMITING_RECIPE_ICON;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(2, 2,this.getTemplate());
        builder.setIngredient(3, 2, this.getBaseItem(entry));
        builder.setIngredient(4, 2, this.getAddition());
        builder.setOutput(6, 2, this.getOutput(entry, player));
    }

    protected ItemStack[] getOutput(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return new ItemStack[] { recipe.getResult(player.server.getRegistryManager()) };
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return this.ingrendients;
    }

    protected Ingredient getBaseItem(@Nullable PolydexEntry entry) {
        return getBase();
    }

    protected abstract Ingredient getTemplate();
    protected abstract Ingredient getAddition();
    protected abstract Ingredient getBase();
}
