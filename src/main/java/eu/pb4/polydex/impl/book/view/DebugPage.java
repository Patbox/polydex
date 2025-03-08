package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DebugPage(Identifier identifier, Identifier entryId) implements PolydexPage {
    @Override
    public Identifier identifier() {
        return identifier;
    }

    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return Items.COMMAND_BLOCK.getDefaultStack();
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return Items.COMMAND_BLOCK.getDefaultStack();
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        layer.setIngredient(0, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 1, 1));
        layer.setIngredient(1, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 1, 0.8f));
        layer.setIngredient(2, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 2, 0.85f));
        layer.setIngredient(3, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 3, 0.543f));
        layer.setIngredient(4, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 4, 0.5432f));
        layer.setIngredient(5, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 5, 0.54321f));
        layer.setIngredient(6, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 6, 0.04321f));
        layer.setIngredient(7, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 7, 0.00321f));
        layer.setIngredient(8, 0, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 999, 0.00321f));
        layer.setIngredient(0, 1, PolydexIngredient.of(Ingredient.ofItems(Items.BRICK), 99, 0.00321f));
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return List.of();
    }

    @Override
    public List<PolydexCategory> categories() {
        return List.of(PolydexCategory.CUSTOM);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.identifier().equals(this.entryId);
    }
}
