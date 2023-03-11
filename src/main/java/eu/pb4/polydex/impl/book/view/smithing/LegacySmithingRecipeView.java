package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.api.PolydexUiElements;
import eu.pb4.polydex.mixin.LegacySmithingRecipeAccessor;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.Items;
import net.minecraft.recipe.LegacySmithingRecipe;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static eu.pb4.polydex.api.PolydexUtils.getIngredientDisplay;

@Deprecated(forRemoval = true)
public final class LegacySmithingRecipeView implements ItemPageView<LegacySmithingRecipe> {
    @Override
    public GuiElement getIcon(ItemEntry entry, LegacySmithingRecipe recipe, ServerPlayerEntity player, Runnable returnCallback) {
        return PolydexUiElements.SMITING_RECIPE_ICON;
    }

    @Override
    public void renderLayer(ItemEntry entry, LegacySmithingRecipe recipe, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        var access = (LegacySmithingRecipeAccessor) recipe;
        layer.setSlot(20, getIngredientDisplay(access.getBase()));
        layer.setSlot(21, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).setName(Text.empty()));
        layer.setSlot(22, getIngredientDisplay(access.getAddition()));
        layer.setSlot(24, recipe.getOutput(player.server.getRegistryManager()));
    }
}
