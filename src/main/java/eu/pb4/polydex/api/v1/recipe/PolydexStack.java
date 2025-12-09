package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.book.PolydexItemStackImpl;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface PolydexStack<T> extends PolydexIngredient<T> {
    PolydexStack<?> EMPTY = of(Items.AIR);
    PolydexStack<ItemStack> EMPTY_STACK = of(Items.AIR);

    @Override
    default List<PolydexStack<T>> asStacks() {
        return List.of(this);
    }

    @Override
    default Optional<PolydexStack<T>> asFirstStack() {
        return Optional.of(this);
    }

    ItemStack toItemStack(ServerPlayer player);
    default ItemStack toDisplayItemStack(ServerPlayer player) {
        return toItemStack(player);
    }
    default ItemStack toTypeDisplayItemStack(ServerPlayer player) {
        return toItemStack(player);
    }

    T getBacking();

    @Nullable
    default <E> E get(DataComponentType<E> type) {
        return null;
    }

    default <E> E getOrDefault(DataComponentType<E> type, E fallback) {
        return fallback;
    }

    default boolean contains(DataComponentType<?> type) {
        return false;
    }

    @Override
    default boolean matches(PolydexStack<?> stack, boolean strict) {
        return this.matchesInternal(stack, strict) && stack.matchesInternal(this, strict);
    }

    static PolydexStack<ItemStack> of(Item item) {
        return of(item.getDefaultInstance(), 1 ,1);
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

    Component getName();

    default int getSourceHashCode() {
        return this.getBacking().hashCode();
    };

    default List<Component> getTexts(ServerPlayer player) {
        return List.of(getName());
    }

    @Nullable
    default Identifier getId() {
        return null;
    }

    default Stream<TagKey<?>> streamTags() {
        return Stream.empty();
    }
}
