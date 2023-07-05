package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.api.PageView;
import eu.pb4.polydex.api.recipe.PageBuilder;
import eu.pb4.polydex.api.recipe.PageIcons;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCraftingRecipeView<T extends CraftingRecipe> implements PageView<T> {
    private static ItemStack CRAFTING_TABLE = PageIcons.CRAFTING_TABLE_RECIPE_ICON;
    private static ItemStack CRAFTING = PageIcons.CRAFTING_RECIPE_ICON;

    @Override
    public ItemStack getIcon(ItemEntry entry, T recipe, ServerPlayerEntity player) {
        return recipe.fits(2, 2) ? CRAFTING : CRAFTING_TABLE;
    }

    @Override
    public void createPage(ItemEntry entry, T recipe, ServerPlayerEntity player, PageBuilder builder) {
        for (int i = 0; i < 9; i++) {
            var x = i % 3;
            var y = i / 3;

            builder.setIngredient(x + 2,  y + 1, getStacksAt(recipe, x, y));
        }

        builder.setOutput(6, 2, getOutput(recipe, player));
    }

    protected ItemStack[] getOutput(T recipe, ServerPlayerEntity player) {
        return new ItemStack[] { recipe.getOutput(player.server.getRegistryManager()) };
    }

    @Override
    public List<Ingredient> getIngredients(T object) {
        var list = new ArrayList<Ingredient>(9);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                var ig = this.getStacksAt(object, x, y);

                if (!ig.isEmpty()) {
                    list.add(ig);
                }
            }
        }
        return list;
    }

    protected abstract Ingredient getStacksAt(T recipe, int x, int y);

    public static <T extends CraftingRecipe> AbstractCraftingRecipeView<T> of(StackGetter<T> getter) {
        return new AbstractCraftingRecipeView<T>() {
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
