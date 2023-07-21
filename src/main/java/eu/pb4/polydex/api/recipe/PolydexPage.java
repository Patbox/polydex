package eu.pb4.polydex.api.recipe;

import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface PolydexPage {
    Identifier identifier();
    ItemStack getIcon(ServerPlayerEntity player);
    @Nullable
    default Text getTexture(ServerPlayerEntity player) {
        return null;
    }
    void createPage(PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer);

    default boolean canDisplay(PolydexEntry entry, ServerPlayerEntity player) {
        return true;
    }

    default int priority() {
        return 0;
    }

    List<PolydexIngredient<?>> getIngredients();

    @ApiStatus.OverrideOnly
    boolean isOwner(MinecraftServer server, PolydexEntry entry);

    static <T extends Recipe<?>> void registerRecipeViewer(Class<T> recipeClass, Function<T, PolydexPage> viewCreator) {
        //noinspection unchecked
        PolydexImpl.RECIPE_VIEWS.put(recipeClass, (Function<Recipe, PolydexPage>) (Object) viewCreator);
    }

    static void register(Creator viewBuilder) {
        PolydexImpl.VIEWS.add(viewBuilder);
    }

    @FunctionalInterface
    interface Creator {
        @Nullable Collection<PolydexPage> createPages(MinecraftServer server, PolydexEntry itemEntry);
    }
}
