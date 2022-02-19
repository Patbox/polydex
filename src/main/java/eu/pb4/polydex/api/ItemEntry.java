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

public record ItemEntry(Identifier identifier, Item item, ItemStack stack, List<PageEntry<?>> pages) {
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


    public static ItemEntry of(Item item) {
        return new ItemEntry(Registry.ITEM.getId(item), item, item.getDefaultStack(), new ArrayList<>());
    }

    public static ItemEntry of(Item item, ItemStack stack) {
        return new ItemEntry(Registry.ITEM.getId(item),item, stack, new ArrayList<>());
    }

    public static ItemEntry of(Identifier identifier, Item item, ItemStack stack) {
        return new ItemEntry(identifier, item, stack, new ArrayList<>());
    }

    public static void registerBuilder(Item item, Function<Item, @Nullable Collection<ItemEntry>> builder) {
        PolydexImpl.ITEM_ENTRY_BUILDERS.put(item, builder);
    }
}
