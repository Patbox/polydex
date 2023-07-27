package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.book.PolydexIngredientImpl;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public interface PolydexIngredient<T> {
    List<PolydexStack<T>> asStacks();
    float chance();
    long amount();

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
        return new PolydexIngredientImpl(ingredient, 1, 1);
    }

    static PolydexIngredient<ItemStack> of(Ingredient ingredient, long count) {
        return new PolydexIngredientImpl(ingredient, count, 1);
    }

    static PolydexIngredient<ItemStack> of(Ingredient ingredient, long count, float chance) {
        return new PolydexIngredientImpl(ingredient, count, chance);
    }


}
