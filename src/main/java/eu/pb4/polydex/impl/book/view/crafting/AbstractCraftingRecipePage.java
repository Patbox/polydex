package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class AbstractCraftingRecipePage<T extends CraftingRecipe> extends AbstractRecipePolydexPage<T> {
    private static ItemStack CRAFTING_TABLE = PageIcons.CRAFTING_TABLE_RECIPE_ICON;
    private static ItemStack CRAFTING = PageIcons.CRAFTING_RECIPE_ICON;

    public AbstractCraftingRecipePage(RecipeEntry<T> recipe) {
        super(recipe);
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return InternalPageTextures.CRAFTING;
    }

    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return recipe.fits(2, 2) ? CRAFTING : CRAFTING_TABLE;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        for (int i = 0; i < 9; i++) {
            var x = i % 3;
            var y = i / 3;

            builder.setIngredient(x + 2,  y + 1, getStacksAt(recipe, x, y));
        }

        builder.setOutput(6, 2, getOutput(recipe, player));
    }

    protected ItemStack[] getOutput(T recipe, ServerPlayerEntity player) {
        return new ItemStack[] { recipe.getResult(player.server.getRegistryManager()) };
    }

    @Override
    public boolean syncWithClient(ServerPlayerEntity player) {
        return false;
    }

    protected abstract Ingredient getStacksAt(T recipe, int x, int y);

    public static <T extends CraftingRecipe> Function<RecipeEntry<T>, AbstractCraftingRecipePage<T>> of(StackGetter<T> getter) {
        return (r) -> new AbstractCraftingRecipePage<T>(r) {
            @Override
            protected Ingredient getStacksAt(T recipe, int x, int y) {
                return getter.getStacksAt(recipe, x, y);
            }
        };
    };

    public interface StackGetter<T> {
        Ingredient getStacksAt(T recipe, int x, int y);
    }
}
