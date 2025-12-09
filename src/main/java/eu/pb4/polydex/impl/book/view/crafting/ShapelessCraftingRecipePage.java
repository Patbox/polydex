package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.mixin.ShapelessRecipeAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ShapelessCraftingRecipePage extends AbstractCraftingRecipePage<ShapelessRecipe> {
    public ShapelessCraftingRecipePage(RecipeHolder<ShapelessRecipe> recipe) {
        super(recipe);
    }

    protected SlotDisplay getStacksAt(ShapelessRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        var list = recipe.placementInfo().ingredients();
        var i = x + y * 3;
        if (i < list.size()) {
            return list.get(i).display();
        }
        return SlotDisplay.Empty.INSTANCE;
    };

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((ShapelessRecipeAccessor) this.recipe).getResult();
    }
}
