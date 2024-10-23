package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.mixin.ShapedRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ShapedCraftingRecipePage extends AbstractCraftingRecipePage<ShapedRecipe> {
    public ShapedCraftingRecipePage(RecipeEntry<ShapedRecipe> recipe) {
        super(recipe);
    }

    protected SlotDisplay getStacksAt(ShapedRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        if (x < recipe.getWidth() && y < recipe.getHeight()) {
            return recipe.getIngredients().get(x + (recipe.getWidth() * y)).map(Ingredient::toDisplay).orElse(SlotDisplay.EmptySlotDisplay.INSTANCE);
        }
        return SlotDisplay.EmptySlotDisplay.INSTANCE;
    };

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((ShapedRecipeAccessor) this.recipe).getResult().copy();
    }
}
