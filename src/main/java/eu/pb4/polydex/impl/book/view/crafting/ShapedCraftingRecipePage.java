package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.mixin.ShapedRecipeAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ShapedCraftingRecipePage extends AbstractCraftingRecipePage<ShapedRecipe> {
    public ShapedCraftingRecipePage(RecipeHolder<ShapedRecipe> recipe) {
        super(recipe);
    }

    protected SlotDisplay getStacksAt(ShapedRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        if (x < recipe.getWidth() && y < recipe.getHeight()) {
            return recipe.getIngredients().get(x + (recipe.getWidth() * y)).map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE);
        }
        return SlotDisplay.Empty.INSTANCE;
    };

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((ShapedRecipeAccessor) this.recipe).getResult().copy();
    }
}
