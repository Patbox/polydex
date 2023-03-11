package eu.pb4.polydex.api;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

public interface ItemPageView<T> {
    GuiElement getIcon(ItemEntry entry, T object, ServerPlayerEntity player, Runnable returnCallback);
    void renderLayer(ItemEntry entry, T object, ServerPlayerEntity player, Layer layer, Runnable returnCallback);

    default PageEntry<T> toEntry(T entry) {
        return new PageEntry<>(this, entry);
    }

    default boolean canDisplay(ItemEntry entry, T object, ServerPlayerEntity player) {
        return true;
    }

    default List<Ingredient> getIngredients(T object) {
        if (object instanceof Recipe<?> recipe) {
            return recipe.getIngredients();
        }

        return List.of();
    }

    static <T extends Recipe<?>> void registerRecipeViewer(Class<T> recipeClass, ItemPageView<T> view) {
        PolydexImpl.RECIPE_VIEWS.put(recipeClass, (ItemPageView<Recipe<?>>) view);
    }

    @Deprecated
    static <T extends Recipe<?>> void registerRecipe(RecipeType<T> recipeType, ItemPageView<T> view) {
        PolydexImpl.RECIPE_TYPE_VIEWS.put(recipeType, (ItemPageView<Recipe<?>>) view);
    }

    static void register(BiFunction<MinecraftServer, ItemEntry, @Nullable Collection<PageEntry<?>>> viewBuilder) {
        PolydexImpl.VIEWS.add(((server, itemEntry, recipeManager) -> viewBuilder.apply(server, itemEntry)));
    }

    static void register(PageEntryCreator viewBuilder) {
        PolydexImpl.VIEWS.add(viewBuilder);
    }

    @FunctionalInterface
    interface PageEntryCreator {
        @Nullable Collection<PageEntry<?>> createEntries(MinecraftServer server, ItemEntry itemEntry, Collection<Recipe<?>> recipes);
    }
}
