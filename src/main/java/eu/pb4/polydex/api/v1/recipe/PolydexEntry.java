package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.PolydexEntryImpl;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

@ApiStatus.NonExtendable
public interface PolydexEntry {
    static PolydexEntry of(Item item) {
        return new PolydexEntryImpl(Registries.ITEM.getId(item), PolydexStack.of(item), new ArrayList<>(), new ArrayList<>(), PolydexEntryImpl.WEAK_CHECK);
    }

    static PolydexEntry of(ItemStack stack) {
        return new PolydexEntryImpl(Registries.ITEM.getId(stack.getItem()), PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), PolydexEntryImpl.WEAK_CHECK);
    }

    static PolydexEntry of(Identifier identifier, ItemStack stack) {
        return new PolydexEntryImpl(identifier, PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), PolydexEntryImpl.STRICT_CHECK);
    }

    static PolydexEntry of(Identifier identifier, ItemStack stack, BiPredicate<PolydexEntry, PolydexStack<?>> isPartOf) {
        return new PolydexEntryImpl(identifier, PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), isPartOf);
    }

    static void registerEntryCreator(Item item, Function<ItemStack, @Nullable PolydexEntry> builder) {
        PolydexImpl.ITEM_ENTRY_CREATOR.put(item, builder);
    }

    static void registerBuilder(Item item, Function<Item, @Nullable Collection<PolydexEntry>> builder) {
        PolydexImpl.ITEM_ENTRY_BUILDERS.put(item, builder);
    }

    Identifier identifier();

    PolydexStack<?> stack();

    List<PolydexPage> outputPages();

    List<PolydexPage> ingredientPages();

    default int getVisiblePagesSize(ServerPlayerEntity player) {
        int i = 0;
        for (var page : this.outputPages()) {
            if (page.canDisplay(this, player)) {
                i++;
            }
        }

        return i;
    }

    default List<PolydexPage> getVisiblePages(ServerPlayerEntity player) {
        var list = new ArrayList<PolydexPage>();
        for (var page : this.outputPages()) {
            if (page.canDisplay(this, player)) {
                list.add(page);
            }
        }

        return list;
    }

    default List<PolydexPage> getVisibleIngredientPages(ServerPlayerEntity player) {
        var list = new ArrayList<PolydexPage>();
        for (var page : this.ingredientPages()) {
            if (page.canDisplay(this, player)) {
                list.add(page);
            }
        }

        return list;
    }

    default int getVisibleIngredientPagesSize(ServerPlayerEntity player) {
        int i = 0;
        for (var page : this.ingredientPages()) {
            if (page.canDisplay(this, player)) {
                i++;
            }
        }
        return i;
    }

    boolean isPartOf(PolydexStack<?> stack);

}
