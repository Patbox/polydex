package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.mixin.TransmuteRecipeAccessor;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.TransmuteRecipe;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TransmuteRecipePage extends AbstractCraftingRecipePage<TransmuteRecipe> {

    public TransmuteRecipePage(RecipeEntry<TransmuteRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected SlotDisplay getStacksAt(TransmuteRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        var list = recipe.getIngredientPlacement().getIngredients();
        var i = x + y * 3;
        if (i < list.size()) {
            return list.get(i).toDisplay();
        }
        return SlotDisplay.EmptySlotDisplay.INSTANCE;
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((TransmuteRecipeAccessor) this.recipe).getResult().value().getDefaultStack();
    }
}
