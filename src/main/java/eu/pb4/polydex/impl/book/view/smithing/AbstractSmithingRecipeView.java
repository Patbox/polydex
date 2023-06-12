package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.api.PolydexUiElements;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static eu.pb4.polydex.api.PolydexUtils.getIngredientDisplay;

public abstract class AbstractSmithingRecipeView<T extends SmithingRecipe> implements ItemPageView<T> {
    @Override
    public GuiElement getIcon(ItemEntry entry, T recipe, ServerPlayerEntity player, Runnable returnCallback) {
        return PolydexUiElements.SMITING_RECIPE_ICON;
    }

    @Override
    public void renderLayer(ItemEntry entry, T recipe, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        layer.setSlot(20, getIngredientDisplay(this.getBase(recipe)));
        layer.setSlot(21, getIngredientDisplay(this.getTemplate(recipe)));
        layer.setSlot(22, getIngredientDisplay(this.getAddition(recipe)));
        layer.setSlot(24, recipe.getOutput(player.server.getRegistryManager()));
    }

    protected abstract ItemStack[] getTemplate(T recipe);
    protected abstract ItemStack[] getAddition(T recipe);
    protected abstract ItemStack[] getBase(T recipe);
}
