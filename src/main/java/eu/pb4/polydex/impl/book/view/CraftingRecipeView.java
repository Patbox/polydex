package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.PolydexUtils;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

public final class CraftingRecipeView implements ItemPageView<CraftingRecipe> {
    private static GuiElement CRAFTING_TABLE = new GuiElement(Items.CRAFTING_TABLE.getDefaultStack(), GuiElement.EMPTY_CALLBACK);
    private static GuiElement CRAFTING = new GuiElementBuilder(Items.CRAFTING_TABLE)
            .setName(Text.translatable("block.minecraft.crafting_table").append(" / ").append(Text.translatable("text.polydex.recipe.player_crafting")))
            .build();

    @Override
    public GuiElement getIcon(ItemEntry entry, CraftingRecipe recipe, ServerPlayerEntity player, Runnable returnCallback) {
        return recipe.fits(2, 2) ? CRAFTING : CRAFTING_TABLE;
    }

    @Override
    public void renderLayer(ItemEntry entry, CraftingRecipe recipe, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        DefaultedList<Ingredient> ingredients = recipe.getIngredients();

        for (int i = 0; i < 9; i++) {
            ItemStack[] stacks = new ItemStack[0];

            if (recipe instanceof ShapedRecipe shapedRecipe) {
                if (i % 3 < shapedRecipe.getWidth() && i / 3 < shapedRecipe.getHeight()) {
                    stacks = PolydexUtils.readIngredient(ingredients.get(i % 3 + (shapedRecipe.getWidth() * (i / 3))));
                }
            } else if (ingredients.size() > i && recipe instanceof ShapelessRecipe) {
                stacks = PolydexUtils.readIngredient(ingredients.get(i));
            }

            layer.setSlot(i % 3 + (9 * (i / 3)) + 11, PolydexUtils.getIngredientDisplay(stacks));
        }


        layer.setSlot(24, new GuiElement(recipe.getOutput(), GuiElement.EMPTY_CALLBACK));

    }
}
