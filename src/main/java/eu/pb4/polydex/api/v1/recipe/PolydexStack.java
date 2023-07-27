package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.book.PolydexItemStackImpl;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public interface PolydexStack<T> extends PolydexIngredient<T> {
    @Override
    default List<PolydexStack<T>> asStacks() {
        return List.of(this);
    }

    ItemStack toItemStack(ServerPlayerEntity player);

    T getBacking();

    @Override
    default boolean matches(PolydexStack<?> stack, boolean strict) {
        return this.matchesInternal(stack, strict) && stack.matchesInternal(this, strict);
    }

    static PolydexStack<ItemStack> of(Item item) {
        return new PolydexItemStackImpl(item.getDefaultStack(), 1);
    }

    static PolydexStack<ItemStack> of(ItemStack stack) {
        return new PolydexItemStackImpl(stack, 1);
    }

    static PolydexStack<ItemStack> of(ItemStack stack, float chance) {
        return new PolydexItemStackImpl(stack, chance);
    }

    boolean isEmpty();

    Text getName();
}
