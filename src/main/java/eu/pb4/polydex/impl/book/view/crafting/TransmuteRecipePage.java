package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.mixin.TransmuteRecipeAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TransmuteRecipePage extends AbstractCraftingRecipePage<TransmuteRecipe> {
    private final PolydexIngredient<ItemStack> inputIngredient;
    private final boolean changesType;

    public TransmuteRecipePage(RecipeHolder<TransmuteRecipe> recipe) {
        super(recipe);
        this.inputIngredient = PolydexIngredient.of(((TransmuteRecipeAccessor) recipe.value()).getInput());
        this.changesType = ((TransmuteRecipeAccessor) recipe.value()).getResult().item().isPresent();
    }

    @Override
    protected SlotDisplay getStacksAt(TransmuteRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        var list = recipe.placementInfo().ingredients();
        var i = x + y * 3;
        if (i < list.size()) {
            return list.get(i).display();
        }
        return SlotDisplay.Empty.INSTANCE;
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        var e = entry != null && entry.stack().getBacking() instanceof ItemStack stack ? stack : Items.STONE.getDefaultInstance();
        return TransmuteRecipe.createWithOriginalComponents(((TransmuteRecipeAccessor) this.recipe).getResult().resolve(e.typeHolder()), e);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        if (this.changesType) {
            return super.isOwner(server, entry);
        }

        return this.inputIngredient.matches(entry.stack(), false);
    }
}
