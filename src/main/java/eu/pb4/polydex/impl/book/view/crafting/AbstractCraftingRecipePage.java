package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public abstract class AbstractCraftingRecipePage<T extends CraftingRecipe> extends AbstractRecipePolydexPage<T> {
    private static final ItemStack CRAFTING_TABLE = PageIcons.CRAFTING_TABLE_RECIPE_ICON;
    private static final ItemStack CRAFTING = PageIcons.CRAFTING_RECIPE_ICON;

    public AbstractCraftingRecipePage(RecipeHolder<T> recipe) {
        super(recipe);
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return InternalPageTextures.CRAFTING;
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return CRAFTING;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder builder) {
        for (int i = 0; i < 9; i++) {
            var x = i % 3;
            var y = i / 3;

            builder.setIngredient(x + 2,  y + 1, getStacksAt(recipe, x, y, entry));
        }

        builder.setOutput(6, 2, getOutput(entry, player.level().getServer()));
    }

    @Override
    public boolean syncWithClient(ServerPlayer player) {
        return false;
    }


    protected abstract SlotDisplay getStacksAt(T recipe, int x, int y, @Nullable PolydexEntry entry);

    /*public static <T extends CraftingRecipe> Function<RecipeEntry<T>, AbstractCraftingRecipePage<T>> of(StackGetter<T> getter) {
        return (r) -> new AbstractCraftingRecipePage<T>(r) {
            @Override
            protected Ingredient getStacksAt(T recipe, int x, int y, @Nullable PolydexEntry entry) {
                return getter.getStacksAt(recipe, x, y);
            }
        };
    };*/

    public interface StackGetter<T> {
        Ingredient getStacksAt(T recipe, int x, int y);
    }
}
