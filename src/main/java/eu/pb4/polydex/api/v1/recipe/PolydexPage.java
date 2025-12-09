package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface PolydexPage {
    Identifier identifier();
    ItemStack typeIcon(ServerPlayer player);
    default ItemStack typeIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        return typeIcon(player);
    }
    ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player);
    @Nullable
    default Component texture(ServerPlayer player) {
        return null;
    }
    void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder layer);

    default boolean canDisplay(@Nullable PolydexEntry entry, ServerPlayer player) {
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

    static <T extends Recipe<?>> void registerRecipeViewer(Class<T> recipeClass, Function<RecipeHolder<T>, PolydexPage> viewCreator) {
        PolydexImpl.RECIPE_VIEWS.put(recipeClass, (Function<RecipeHolder<?>, PolydexPage>) (Object) viewCreator);
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

    default boolean syncWithClient(ServerPlayer player) {
        return true;
    }
}
