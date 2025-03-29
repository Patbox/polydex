package eu.pb4.polydex.impl.book;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polydex.mixin.IngredientAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class PolydexIngredientImpl implements PolydexIngredient<ItemStack> {
    private static final Interner<PolydexIngredientImpl> INTERNER = Interners.newWeakInterner();
    private final Ingredient ingredient;
    private final long count;
    private final float chance;
    private final List<PolydexStack<ItemStack>> polydexStacks;
    private final int ingredientHash;

    public PolydexIngredientImpl(Ingredient ingredient, long count, float chance) {
        this.ingredient = ingredient;
        this.count = count;
        this.chance = chance;
        List<ItemStack> itemStacks;
        if (ingredient.getCustomIngredient() != null) {
            itemStacks = ingredient.getCustomIngredient().getMatchingStacks();
        } else {
            itemStacks = List.of(ingredient.getMatchingStacks());
        }
        this.polydexStacks = itemStacks.stream().map((x) -> PolydexStack.of(x, count, chance)).toList();
        this.ingredientHash = Arrays.hashCode(((IngredientAccessor) (Object) ingredient).getEntries());
    }

    public static PolydexIngredient<ItemStack> of(Ingredient ingredient, long count, float chance) {
        return INTERNER.intern(new PolydexIngredientImpl(ingredient, count, chance));
    }

    @Override
    public boolean isEmpty() {
        return this.ingredient.isEmpty();
    }

    @Override
    public List<PolydexStack<ItemStack>> asStacks() {
        return this.polydexStacks;
    }

    @Override
    public float chance() {
        return this.chance;
    }

    @Override
    public long amount() {
        return this.count;
    }

    @Override
    public boolean matchesInternal(PolydexStack<?> stack, boolean strict) {
        if (stack.getBackingClass() == PolydexItemStackImpl.ITEM_STACK_CLASS) {
            //noinspection unchecked
            return matchesDirect((PolydexStack<ItemStack>) stack, strict);
        }
        return false;
    }

    @Override
    public boolean matchesDirect(PolydexStack<ItemStack> stack, boolean strict) {
        return this.ingredient.test(stack.getBacking());
    }

    @Override
    public Class<ItemStack> getBackingClass() {
        return PolydexItemStackImpl.ITEM_STACK_CLASS;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chance, count, ingredientHash);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PolydexIngredientImpl that = (PolydexIngredientImpl) object;
        return count == that.count && chance == that.chance && Objects.equals(ingredient, that.ingredient) ;
    }
}
