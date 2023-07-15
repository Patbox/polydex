package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.recipe.PolydexIngredient;
import eu.pb4.polydex.api.recipe.PolydexStack;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

import java.util.List;

public class PolydexIngredientImpl implements PolydexIngredient<ItemStack> {

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
        this.polydexStacks = this.itemStacks.stream().map(PolydexStack::of).toList();
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
