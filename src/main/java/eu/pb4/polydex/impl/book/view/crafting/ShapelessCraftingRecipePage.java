package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.mixin.ShapelessRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ShapelessCraftingRecipePage extends AbstractCraftingRecipePage<ShapelessRecipe> {
    public ShapelessCraftingRecipePage(RecipeEntry<ShapelessRecipe> recipe) {
        super(recipe);
    }

    protected SlotDisplay getStacksAt(ShapelessRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        var list = recipe.getIngredientPlacement().getIngredients();
        var i = x + y * 3;
        if (i < list.size()) {
            return list.get(i).toDisplay();
        }
        return SlotDisplay.EmptySlotDisplay.INSTANCE;
    };

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((ShapelessRecipeAccessor) this.recipe).getResult();
    }
}
