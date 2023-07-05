package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.recipe.ItemEntry;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShulkerBoxColoringRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class ShulkerBoxColoringRecipeView extends AbstractCraftingRecipeView<ShulkerBoxColoringRecipe> {
    private static final Ingredient SHULKERS_ANY = Ingredient.ofStacks(Registries.ITEM.stream()
            .filter(x -> x instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock).map(Item::getDefaultStack));
    private static final Ingredient DYES = Ingredient.ofStacks(Registries.ITEM.stream()
            .filter(x -> x instanceof DyeItem).map(Item::getDefaultStack));
    private static final ItemStack[] SHULKERS_DYES = Registries.ITEM.stream()
            .filter(x -> x instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock && shulkerBoxBlock.getColor() != null)
            .map(Item::getDefaultStack).toArray(ItemStack[]::new);

    @Override
    protected Ingredient getStacksAt(ShulkerBoxColoringRecipe recipe, int x, int y) {
        return y == 0 ? switch (x) {
            case 0 -> SHULKERS_ANY;
            case 1 -> DYES;
            default -> Ingredient.EMPTY;
        } : Ingredient.EMPTY;
    }

    @Override
    protected ItemStack[] getOutput(ShulkerBoxColoringRecipe recipe, ServerPlayerEntity player) {
        return SHULKERS_DYES;
    }

    @Override
    public boolean isOwner(MinecraftServer server, ItemEntry entry, ShulkerBoxColoringRecipe object) {
        return entry.item() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock && shulkerBoxBlock.getColor() != null;
    }
}
