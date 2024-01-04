package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface PolydexPage {
    Identifier identifier();
    ItemStack typeIcon(ServerPlayerEntity player);
    default ItemStack typeIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return typeIcon(player);
    }
    ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player);
    @Nullable
    default Text texture(ServerPlayerEntity player) {
        return null;
    }
    void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer);

    default boolean canDisplay(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return true;
    }

    default int priority() {
        return 0;
    }

    List<PolydexIngredient<?>> ingredients();
    List<PolydexCategory> categories();

    default String sortingId() {
        var group = this.getGroup();

        if (group.isEmpty()) {
            return this.identifier().getPath() + "|" + this.identifier().getNamespace();
        }

        return group + "|" + this.identifier().getPath() + "|" + this.identifier().getNamespace();
    }

    default String getGroup() {
        return "";
    }

    @ApiStatus.OverrideOnly
    boolean isOwner(MinecraftServer server, PolydexEntry entry);

    static <T extends Recipe<?>> void registerRecipeViewer(Class<T> recipeClass, Function<RecipeEntry<T>, PolydexPage> viewCreator) {
        PolydexImpl.RECIPE_VIEWS.put(recipeClass, (Function<RecipeEntry<?>, PolydexPage>) (Object) viewCreator);
    }

    static void registerModifier(EntryModifier viewBuilder) {
        PolydexImpl.ENTRY_MODIFIERS.add(viewBuilder);
    }

    static void register(PageCreator creator) {
        PolydexImpl.PAGE_CREATORS.add(creator);
    }

    @FunctionalInterface
    interface EntryModifier {
        void entryModifier(MinecraftServer server, PolydexEntry itemEntry);
    }

    @FunctionalInterface
    interface PageCreator {
        void createPages(MinecraftServer server, Consumer<PolydexPage> pageConsumer);
    }

    default boolean syncWithClient(ServerPlayerEntity player) {
        return true;
    }
}
