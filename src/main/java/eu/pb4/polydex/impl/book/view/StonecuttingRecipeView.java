package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.Items;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import static eu.pb4.polydex.api.PolydexUtils.getIngredientDisplay;

public final class StonecuttingRecipeView implements ItemPageView<StonecuttingRecipe> {
    @Override
    public GuiElement getIcon(ItemEntry entry, StonecuttingRecipe recipe, ServerPlayerEntity player, Runnable returnCallback) {
        return new GuiElement(Items.STONECUTTER.getDefaultStack(), GuiElement.EMPTY_CALLBACK);
    }

    @Override
    public void renderLayer(ItemEntry entry, StonecuttingRecipe recipe, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        layer.setSlot(20, getIngredientDisplay(recipe.getIngredients().get(0)));
        layer.setSlot(22, new GuiElementBuilder(Items.ARROW).setName(LiteralText.EMPTY));
        layer.setSlot(24, new GuiElement(recipe.getOutput(), GuiElement.EMPTY_CALLBACK));
    }
}
