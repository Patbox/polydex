package eu.pb4.polydex.impl.book;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

import java.util.List;

public class PolydexIngredientImpl implements PolydexIngredient<ItemStack> {
    private static final Interner<PolydexIngredientImpl> INTERNER = Interners.newWeakInterner();
    private final Ingredient ingredient;
    private final long count;
    private final float chance;
    private final List<ItemStack> itemStacks;
    private final List<PolydexStack<ItemStack>> polydexStacks;

    public PolydexIngredientImpl(Ingredient ingredient, long count, float chance) {
        this.ingredient = ingredient;
        this.count = count;
        this.chance = chance;
        if (ingredient.getCustomIngredient() != null) {
            this.itemStacks = ingredient.getCustomIngredient().getMatchingStacks();
        } else {
            this.itemStacks = List.of(ingredient.getMatchingStacks());
        }
        this.polydexStacks = this.itemStacks.stream().map((x) -> PolydexStack.of(x, count, chance)).toList();
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
    public boolean matchesDirect(PolydexStack<ItemStack> stack, boolean strict) {
        return this.ingredient.test(stack.getBacking());
    }

    @Override
    public Class<ItemStack> getBackingClass() {
        return ItemStack.class;
    }

}
