package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public class PolydexItemStackImpl implements PolydexStack<ItemStack> {
    private final ItemStack stack;
    private final float chance;
    private final long count;

    public PolydexItemStackImpl(ItemStack stack, long count, float chance) {
        this.stack = stack.copyWithCount((int) Math.min(count, 64));
        this.chance = chance;
        this.count = count;
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
        if (this.count == this.stack.getCount() && this.chance == 1) {
            return this.stack.copy();
        } else {
            var lore = new ArrayList<Text>();
            try {
                lore.addAll(this.stack.getTooltip(player, TooltipContext.BASIC));
                lore.remove(0);
            } catch (Throwable e) {}

            Text extra;

            if (this.count > 64 && chance != 1) {
               extra = Text.translatable("text.polydex.item_stack.count_chance",
                       Text.literal("" + this.count).formatted(Formatting.WHITE),
                       Text.literal(String.format("%.2f%%", this.chance)).formatted(Formatting.WHITE)
               ).formatted(Formatting.YELLOW);
            } else if (this.count > 64) {
                extra = Text.translatable("text.polydex.item_stack.count",
                        Text.literal("" + this.count).formatted(Formatting.WHITE)
                ).formatted(Formatting.YELLOW);
            } else if (chance != 1) {
                extra = Text.translatable("text.polydex.item_stack.chance",
                        Text.literal(String.format("%.2f%%", this.chance)).formatted(Formatting.WHITE)
                ).formatted(Formatting.YELLOW);
            } else {
                extra = null;
            }

            if (extra != null) {
                lore.add(
                        Text.empty()
                                .append(Text.literal("[").formatted(Formatting.DARK_GRAY))
                                .append(extra)
                                .append(Text.literal("]").formatted(Formatting.DARK_GRAY))
                );
            }

            return GuiElementBuilder.from(this.stack)
                    .hideFlags()
                    .setCount(this.count > 64 ? 1 : (int) this.count)
                    .setLore(lore)
                    .asStack();
        }
    }

    @Override
    public ItemStack getBacking() {
        return this.stack;
    }
}
