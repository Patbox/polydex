package eu.pb4.polydex.api;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public record PageEntry<T>(ItemPageView<T> view, T object) {
    public GuiElement getIcon(ItemEntry entry, ServerPlayerEntity player, Runnable returnCallback) {
        try {
            return this.view.getIcon(entry, object, player, returnCallback);
        } catch (Throwable e) {
            e.printStackTrace();
            return PolydexUiElements.INVALID_PAGE;
        }
    }

    public void renderLayer(ItemEntry entry, Layer layer, ServerPlayerEntity player, Runnable returnCallback) {
        try {
            this.view.renderLayer(entry, this.object, player, layer, returnCallback);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean canDisplay(ItemEntry entry, ServerPlayerEntity player) {
        try {
            return this.view.canDisplay(entry, this.object, player);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Ingredient> getIngredients() {
        try {
            return this.view.getIngredients(this.object);
        } catch (Throwable e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
