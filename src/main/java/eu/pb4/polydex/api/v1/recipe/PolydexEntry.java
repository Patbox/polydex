package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.PolydexEntryImpl;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

@ApiStatus.NonExtendable
public interface PolydexEntry {
    static PolydexEntry of(Item item) {
        return new PolydexEntryImpl(BuiltInRegistries.ITEM.getKey(item), PolydexStack.of(item), new ArrayList<>(), new ArrayList<>(), PolydexEntryImpl.WEAK_CHECK);
    }

    static PolydexEntry of(ItemStack stack) {
        return new PolydexEntryImpl(BuiltInRegistries.ITEM.getKey(stack.getItem()), PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), PolydexEntryImpl.WEAK_CHECK);
    }

    static PolydexEntry of(Identifier identifier, ItemStack stack) {
        return new PolydexEntryImpl(identifier, PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), PolydexEntryImpl.STRICT_CHECK);
    }

    static PolydexEntry of(Identifier identifier, PolydexStack<?> stack) {
        return new PolydexEntryImpl(identifier, stack, new ArrayList<>(), new ArrayList<>(), PolydexEntryImpl.STRICT_CHECK);
    }

    static PolydexEntry of(Identifier identifier, ItemStack stack, BiPredicate<PolydexEntry, PolydexStack<?>> isPartOf) {
        return new PolydexEntryImpl(identifier, PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), isPartOf);
    }

    static PolydexEntry of(Identifier identifier, PolydexStack<?> stack, BiPredicate<PolydexEntry, PolydexStack<?>> isPartOf) {
        return new PolydexEntryImpl(identifier, stack, new ArrayList<>(), new ArrayList<>(), isPartOf);
    }

    static void registerEntryCreator(Item item, Function<ItemStack, @Nullable PolydexEntry> builder) {
        PolydexImpl.ITEM_ENTRY_CREATOR.put(item, builder);
    }

    static void registerProvider(BiConsumer<MinecraftServer, EntryConsumer> builder) {
        PolydexImpl.ENTRY_PROVIDERS.add(builder);
    }

    static void registerBuilder(Item item, Function<Item, @Nullable Collection<PolydexEntry>> builder) {
        PolydexImpl.ITEM_ENTRY_BUILDERS.put(item, builder);
    }

    Identifier identifier();

    PolydexStack<?> stack();

    List<PolydexPage> outputPages();

    List<PolydexPage> ingredientPages();

    default int getVisiblePagesSize(ServerPlayer player) {
        int i = 0;
        for (var page : this.outputPages()) {
            if (page.canDisplay(this, player)) {
                i++;
            }
        }

        return i;
    }

    default List<PolydexPage> getVisiblePages(ServerPlayer player) {
        var list = new ArrayList<PolydexPage>();
        for (var page : this.outputPages()) {
            if (page.canDisplay(this, player)) {
                list.add(page);
            }
        }

        return list;
    }

    default List<PolydexPage> getVisibleIngredientPages(ServerPlayer player) {
        var list = new ArrayList<PolydexPage>();
        for (var page : this.ingredientPages()) {
            if (page.canDisplay(this, player)) {
                list.add(page);
            }
        }

        return list;
    }

    default int getVisibleIngredientPagesSize(ServerPlayer player) {
        int i = 0;
        for (var page : this.ingredientPages()) {
            if (page.canDisplay(this, player)) {
                i++;
            }
        }
        return i;
    }

    boolean isPartOf(PolydexStack<?> stack);

    default boolean hasPages() {
        return !this.outputPages().isEmpty() || !this.ingredientPages().isEmpty();
    }


    interface EntryConsumer extends Consumer<PolydexEntry> {
        void accept(PolydexEntry entry);
        void accept(PolydexEntry entry, CreativeModeTab group);
        void acceptAll(Collection<PolydexEntry> entries);
        void acceptAll(Collection<PolydexEntry> entries, CreativeModeTab group);
    }
}
