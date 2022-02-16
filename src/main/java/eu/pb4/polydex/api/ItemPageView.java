package eu.pb4.polydex.api;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiFunction;

public interface ItemPageView<T> {
    GuiElement getIcon(ItemEntry entry, T object, ServerPlayerEntity player, Runnable returnCallback);
    void renderLayer(ItemEntry entry, T object, ServerPlayerEntity player, Layer layer, Runnable returnCallback);

    default PageEntry<T> toEntry(T entry) {
        return new PageEntry<>(this, entry);
    }

    static <T extends Recipe<?>> void registerRecipe(RecipeType<T> recipeType, ItemPageView<T> view) {
        PolydexImpl.RECIPE_VIEWS.put(recipeType, (ItemPageView<Recipe<?>>) view);
    }

    static void register(BiFunction<MinecraftServer, ItemEntry, @Nullable Collection<PageEntry<?>>> viewBuilder) {
        PolydexImpl.VIEWS.add(viewBuilder);
    }

}
