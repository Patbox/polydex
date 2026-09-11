package eu.pb4.polydex.impl.book;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polydex.impl.book.view.BrewingRecipePage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PotionIngredient;

import java.util.List;

public class PolydexPotionIngredientImpl implements PolydexIngredient<ItemStack> {
    private static final Interner<PolydexPotionIngredientImpl> INTERNER = Interners.newWeakInterner();
    private final PotionIngredient ingredient;
    private final long count;
    private final float chance;
    private final List<PolydexStack<ItemStack>> polydexStacks;

    public PolydexPotionIngredientImpl(PotionIngredient ingredient, long count, float chance) {
        this.ingredient = ingredient;
        this.count = count;
        this.chance = chance;

        this.polydexStacks = BrewingRecipePage.stackFromPotionIngredient(ingredient).stream().map(x -> PolydexStack.of(x, count, chance)).toList();
    }

    public static PolydexIngredient<ItemStack> of(PotionIngredient ingredient, long count, float chance) {
        return INTERNER.intern(new PolydexPotionIngredientImpl(ingredient, count, chance));
    }

    @Override
    public boolean isEmpty() {
        return false;
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
