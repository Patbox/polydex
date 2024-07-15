package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.book.PolydexItemStackImpl;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface PolydexStack<T> extends PolydexIngredient<T> {
    PolydexStack<?> EMPTY = of(Items.AIR);

    @Override
    default List<PolydexStack<T>> asStacks() {
        return List.of(this);
    }

    @Override
    default Optional<PolydexStack<T>> asFirstStack() {
        return Optional.of(this);
    }

    ItemStack toItemStack(ServerPlayerEntity player);
    default ItemStack toDisplayItemStack(ServerPlayerEntity player) {
        return toItemStack(player);
    }
    default ItemStack toTypeDisplayItemStack(ServerPlayerEntity player) {
        return toItemStack(player);
    }

    T getBacking();

    @Nullable
    default <E> E get(ComponentType<E> type) {
        return null;
    }

    default <E> E getOrDefault(ComponentType<E> type, E fallback) {
        return fallback;
    }

    default boolean contains(ComponentType<?> type) {
        return false;
    }

    @Override
    default boolean matches(PolydexStack<?> stack, boolean strict) {
        return this.matchesInternal(stack, strict) && stack.matchesInternal(this, strict);
    }

    static PolydexStack<ItemStack> of(Item item) {
        return of(item.getDefaultStack(), 1 ,1);
    }

    static PolydexStack<ItemStack> of(ItemStack stack) {
        return of(stack, stack.getCount(), 1);
    }

    static PolydexStack<ItemStack> of(ItemStack stack, float chance) {
        return of(stack, stack.getCount(), chance);
    }

    static PolydexStack<ItemStack> of(ItemStack stack, long count, float chance) {
        return PolydexItemStackImpl.of(stack, count, chance);
    }

    static PolydexStack<ItemStack> of(ItemVariant variant) {
        return of(variant.toStack());
    }

    static PolydexStack<ItemStack> of(ItemVariant variant, float chance) {
        return of(variant.toStack(), chance);
    }

    static PolydexStack<ItemStack> of(ItemVariant variant, long count, float chance) {
        return of(variant.toStack(), count, chance);
    }

    boolean isEmpty();

    Text getName();

    default int getSourceHashCode() {
        return this.getBacking().hashCode();
    };
}
