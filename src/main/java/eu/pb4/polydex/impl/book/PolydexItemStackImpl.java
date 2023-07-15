package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.recipe.PolydexStack;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PolydexItemStackImpl implements PolydexStack<ItemStack> {
    private final ItemStack stack;
    private final float chance;

    public PolydexItemStackImpl(ItemStack stack, float chance) {
        this.stack = stack;
        this.chance = chance;
    }

    @Override
    public float chance() {
        return this.chance;
    }

    @Override
    public long amount() {
        return this.stack.getCount();
    }

    @Override
    public boolean matchesDirect(PolydexStack<ItemStack> stack, boolean strict) {
        return (this.isEmpty() && stack.isEmpty()) || (strict ? ItemStack.canCombine(this.stack, stack.getBacking()) : this.stack.isOf(stack.getBacking().getItem()));
    }

    @Override
    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    @Override
    public Text getName() {
        return this.stack.getName();
    }

    @Override
    public Class<ItemStack> getBackingClass() {
        return ItemStack.class;
    }

    @Override
    public ItemStack toItemStack(ServerPlayerEntity player) {
        return this.stack.copy();
    }

    @Override
    public ItemStack getBacking() {
        return this.stack;
    }
}
