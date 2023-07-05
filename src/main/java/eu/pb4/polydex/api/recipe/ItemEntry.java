package eu.pb4.polydex.api.recipe;

import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

public record ItemEntry(Identifier identifier, Item item, ItemStack stack, List<PageData<?>> recipeOutput, List<PageData<?>> ingredients, BiPredicate<ItemEntry, ItemStack> isPartOf) {
    private static final BiPredicate<ItemEntry, ItemStack> WEAK_CHECK = ((itemEntry, stack1) -> stack1.isOf(itemEntry.item));
    private static final BiPredicate<ItemEntry, ItemStack> STRICT_CHECK = ((itemEntry, stack1) -> stack1.isOf(itemEntry.stack.getItem()) && Objects.equals(stack1.getNbt(), itemEntry.stack.getNbt()));

    public int getVisiblePagesSize(ServerPlayerEntity player) {
        int i = 0;
        for (var page : recipeOutput) {
            if (page.canDisplay(this, player)) {
                i++;
            }
        }

        return i;
    }

    public List<PageData<?>> getVisiblePages(ServerPlayerEntity player) {
        var list = new ArrayList<PageData<?>>();
        for (var page : recipeOutput) {
            if (page.canDisplay(this, player)) {
                list.add(page);
            }
        }

        return list;
    }

    public List<PageData<?>> getVisibleIngredientPages(ServerPlayerEntity player) {
        var list = new ArrayList<PageData<?>>();
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

    public boolean isPartOf(ItemStack stack) {
        return this.isPartOf.test(this, stack);
    }

    public static ItemEntry of(Item item) {
        return new ItemEntry(Registries.ITEM.getId(item), item, item.getDefaultStack(), new ArrayList<>(), new ArrayList<>(), WEAK_CHECK);
    }

    public static ItemEntry of(ItemStack stack) {
        return new ItemEntry(Registries.ITEM.getId(stack.getItem()), stack.getItem(), stack, new ArrayList<>(), new ArrayList<>(), WEAK_CHECK);
    }

    public static ItemEntry of(Identifier identifier, ItemStack stack) {
        return new ItemEntry(identifier, stack.getItem(), stack, new ArrayList<>(), new ArrayList<>(), STRICT_CHECK);
    }

    public static ItemEntry of(Identifier identifier, ItemStack stack, BiPredicate<ItemEntry, ItemStack> isPartOf) {
        return new ItemEntry(identifier, stack.getItem(), stack, new ArrayList<>(), new ArrayList<>(), isPartOf);
    }

    public static void registerBuilder(Item item, Function<Item, @Nullable Collection<ItemEntry>> builder) {
        PolydexImpl.ITEM_ENTRY_BUILDERS.put(item, builder);
    }

}
