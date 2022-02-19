package eu.pb4.polydex.api;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.server.network.ServerPlayerEntity;

public record PageEntry<T>(ItemPageView<T> view, T object) {
    public GuiElement getIcon(ItemEntry entry, ServerPlayerEntity player, Runnable returnCallback) {
        return this.view.getIcon(entry, object, player, returnCallback);
    }

    public void renderLayer(ItemEntry entry, Layer layer, ServerPlayerEntity player, Runnable returnCallback) {
        this.view.renderLayer(entry, this.object, player, layer, returnCallback);
    }

    public boolean canDisplay(ItemEntry entry, ServerPlayerEntity player) {
        return this.view.canDisplay(entry, this.object, player);
    }
}
