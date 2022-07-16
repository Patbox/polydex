package eu.pb4.polydex.api;

import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public record ItemEntry(Identifier identifier, Item item, ItemStack stack, List<PageEntry<?>> pages, List<PageEntry<?>> ingredients) {

    public ItemEntry(Identifier identifier, Item item, ItemStack stack, List<PageEntry<?>> pages) {
        this(identifier, item, stack, pages, new ArrayList<>());
    }
    public int getVisiblePagesSize(ServerPlayerEntity player) {
        int i = 0;
        for (var page : pages) {
            if (page.canDisplay(this, player)) {
                i++;
            }
        }

        return i;
    }

    public List<PageEntry<?>> getVisiblePages(ServerPlayerEntity player) {
        var list = new ArrayList<PageEntry<?>>();
        for (var page : pages) {
            if (page.canDisplay(this, player)) {
                list.add(page);
            }
        }

        return list;
    }

    public List<PageEntry<?>> getVisibleIngredientPages(ServerPlayerEntity player) {
        var list = new ArrayList<PageEntry<?>>();
        for (var page : ingredients) {
            if (page.canDisplay(this, player)) {
                list.add(page);
            }
        }

        return list;
    }

    public int getVisibleIngredientPagesSize(ServerPlayerEntity player) {
        int i = 0;
        for (var page : ingredients) {
            if (page.canDisplay(this, player)) {
                i++;
            }
        }
        return i;
    }

    public static ItemEntry of(Item item) {
        return new ItemEntry(Registry.ITEM.getId(item), item, item.getDefaultStack(), new ArrayList<>(), new ArrayList<>());
    }

    public static ItemEntry of(Item item, ItemStack stack) {
        return new ItemEntry(Registry.ITEM.getId(item),item, stack, new ArrayList<>(), new ArrayList<>());
    }

    public static ItemEntry of(Identifier identifier, Item item, ItemStack stack) {
        return new ItemEntry(identifier, item, stack, new ArrayList<>(), new ArrayList<>());
    }

    public static void registerBuilder(Item item, Function<Item, @Nullable Collection<ItemEntry>> builder) {
        PolydexImpl.ITEM_ENTRY_BUILDERS.put(item, builder);
    }

}
