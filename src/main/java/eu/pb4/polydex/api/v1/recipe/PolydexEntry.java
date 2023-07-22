package eu.pb4.polydex.api.v1.recipe;

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
import java.util.function.BiPredicate;
import java.util.function.Function;

public record PolydexEntry(Identifier identifier, PolydexStack<?> stack, List<PolydexPage> recipeOutput, List<PolydexPage> ingredients, BiPredicate<PolydexEntry, PolydexStack<?>> isPartOf) {
    private static final BiPredicate<PolydexEntry, PolydexStack<?>> WEAK_CHECK = ((itemEntry, stack1) -> stack1.matches(itemEntry.stack, false));
    private static final BiPredicate<PolydexEntry, PolydexStack<?>> STRICT_CHECK = ((itemEntry, stack1) -> stack1.matches(itemEntry.stack, true));

    public int getVisiblePagesSize(ServerPlayerEntity player) {
        int i = 0;
        for (var page : recipeOutput) {
            if (page.canDisplay(this, player)) {
                i++;
            }
        }

        return i;
    }

    public List<PolydexPage> getVisiblePages(ServerPlayerEntity player) {
        var list = new ArrayList<PolydexPage>();
        for (var page : recipeOutput) {
            if (page.canDisplay(this, player)) {
                list.add(page);
            }
        }

        return list;
    }

    public List<PolydexPage> getVisibleIngredientPages(ServerPlayerEntity player) {
        var list = new ArrayList<PolydexPage>();
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

    public boolean isPartOf(PolydexStack<?> stack) {
        return this.isPartOf.test(this, stack);
    }

    public static PolydexEntry of(Item item) {
        return new PolydexEntry(Registries.ITEM.getId(item), PolydexStack.of(item), new ArrayList<>(), new ArrayList<>(), WEAK_CHECK);
    }

    public static PolydexEntry of(ItemStack stack) {
        return new PolydexEntry(Registries.ITEM.getId(stack.getItem()), PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), WEAK_CHECK);
    }

    public static PolydexEntry of(Identifier identifier, ItemStack stack) {
        return new PolydexEntry(identifier, PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), STRICT_CHECK);
    }

    public static PolydexEntry of(Identifier identifier, ItemStack stack, BiPredicate<PolydexEntry, PolydexStack<?>> isPartOf) {
        return new PolydexEntry(identifier, PolydexStack.of(stack), new ArrayList<>(), new ArrayList<>(), isPartOf);
    }

    public static void registerBuilder(Item item, Function<Item, @Nullable Collection<PolydexEntry>> builder) {
        PolydexImpl.ITEM_ENTRY_BUILDERS.put(item, builder);
    }

}
