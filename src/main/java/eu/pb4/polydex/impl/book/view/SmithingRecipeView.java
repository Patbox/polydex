package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.mixin.SmithingRecipeAccessor;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.Items;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static eu.pb4.polydex.api.PolydexUtils.getIngredientDisplay;

public final class SmithingRecipeView implements ItemPageView<SmithingRecipe> {
    @Override
    public GuiElement getIcon(ItemEntry entry, SmithingRecipe recipe, ServerPlayerEntity player, Runnable returnCallback) {
        return new GuiElement(Items.SMITHING_TABLE.getDefaultStack(), GuiElement.EMPTY_CALLBACK);
    }

    @Override
    public void renderLayer(ItemEntry entry, SmithingRecipe recipe, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        var access = (SmithingRecipeAccessor) recipe;
        layer.setSlot(20, getIngredientDisplay(access.getBase()));
        layer.setSlot(21, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).setName(Text.empty()));
        layer.setSlot(22, getIngredientDisplay(access.getAddition()));
        layer.setSlot(24, recipe.getOutput());
    }
}
