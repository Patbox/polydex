package eu.pb4.polydex.api;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.api.recipe.PageData;
import eu.pb4.polydex.api.recipe.PageBuilder;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

public interface PageView<T> {
    ItemStack getIcon(ItemEntry entry, T object, ServerPlayerEntity player);
    void createPage(ItemEntry entry, T object, ServerPlayerEntity player, PageBuilder layer);
    default PageData<T> toEntry(Identifier identifier, T entry) {
        return new PageData<>(identifier, this, entry);
    }

    default boolean canDisplay(ItemEntry entry, T object, ServerPlayerEntity player) {
        return true;
    }

    default int priority(T object) {
        return 0;
    }

    default List<Ingredient> getIngredients(T object) {
        if (object instanceof Recipe<?> recipe) {
            return recipe.getIngredients();
        }

        return List.of();
    }

    @ApiStatus.OverrideOnly
    default boolean isOwner(MinecraftServer server, ItemEntry entry, T object) {
        if (object instanceof Recipe<?> recipe) {
            return entry.isPartOf(recipe.getOutput(server.getRegistryManager()));
        }

        return false;
    }

    static <T extends Recipe<?>> void registerRecipeViewer(Class<T> recipeClass, PageView<T> view) {
        PolydexImpl.RECIPE_VIEWS.put(recipeClass, (PageView<Recipe<?>>) view);
    }

    static void register(BiFunction<MinecraftServer, ItemEntry, @Nullable Collection<PageData<?>>> viewBuilder) {
        PolydexImpl.VIEWS.add(((server, itemEntry, recipeManager) -> viewBuilder.apply(server, itemEntry)));
    }

    static void register(PageEntryCreator viewBuilder) {
        PolydexImpl.VIEWS.add(viewBuilder);
    }

    @FunctionalInterface
    interface PageEntryCreator {
        @Nullable Collection<PageData<?>> createEntries(MinecraftServer server, ItemEntry itemEntry, Collection<Recipe<?>> recipes);
    }
}
