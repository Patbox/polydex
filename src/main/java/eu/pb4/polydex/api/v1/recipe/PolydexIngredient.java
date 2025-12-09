package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.book.PolydexIngredientImpl;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public interface PolydexIngredient<T> {
    List<PolydexStack<T>> asStacks();
    float chance();
    long amount();

    default Optional<PolydexStack<T>> asFirstStack() {
        var x = asStacks();
        if (x.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(x.getFirst());
    };

    default boolean matches(PolydexStack<?> stack, boolean strict) {
        return this.matchesInternal(stack, strict);
    }

    @ApiStatus.OverrideOnly
    default boolean matchesInternal(PolydexStack<?> stack, boolean strict) {
        if (stack.getBackingClass().isAssignableFrom(this.getBackingClass())) {
            return matchesDirect((PolydexStack<T>) stack, strict);
        }
        return false;
    }

    @ApiStatus.OverrideOnly
    boolean matchesDirect(PolydexStack<T> stack, boolean strict);

    boolean isEmpty();

    Class<T> getBackingClass();

    static PolydexIngredient<ItemStack> of(Ingredient ingredient) {
        if (ingredient == null) {
            return PolydexStack.EMPTY_STACK;
        }
        return of(ingredient, 1, 1);
    }

    static PolydexIngredient<ItemStack> of(Ingredient ingredient, long count) {
        if (ingredient == null) {
            return PolydexStack.EMPTY_STACK;
        }
        return of(ingredient, count, 1);
    }

    static PolydexIngredient<ItemStack> of(Ingredient ingredient, long count, float chance) {
        if (ingredient == null) {
            return PolydexStack.EMPTY_STACK;
        }
        return PolydexIngredientImpl.of(ingredient, count, chance);
    }
}
