package eu.pb4.polydex.impl.book;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class PolydexItemStackImpl implements PolydexStack<ItemStack> {
    private static final Interner<PolydexItemStackImpl> INTERNER = Interners.newWeakInterner();
    private static final IdentityHashMap<Item, PolydexItemStackImpl> PLAIN_ITEMS = new IdentityHashMap<>();

    private final ItemStack stack;
    private final float chance;
    private final long count;
    private final int stackHash;

    public PolydexItemStackImpl(ItemStack stack, long count, float chance) {
        this.stack = stack.copyWithCount((int) Math.min(count, 64));
        this.chance = chance;
        this.count = count;
        this.stackHash = ItemStack.hashItemAndComponents(stack);
    }

    public PolydexItemStackImpl(Item item) {
        this.stack = new ItemStack(item);
        this.chance = 1;
        this.count = 1;
        this.stackHash = ItemStack.hashItemAndComponents(stack);
    }

    public static PolydexStack<ItemStack> of(ItemStack stack, long count, float chance) {
        if (count == 1 && chance == 1f && stack.getComponentsPatch().isEmpty()) {
            synchronized (PLAIN_ITEMS) {
                return PLAIN_ITEMS.computeIfAbsent(stack.getItem(), PolydexItemStackImpl::new);
            }
        }

        return INTERNER.intern(new PolydexItemStackImpl(stack, count, chance));
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
        return (this.isEmpty() && stack.isEmpty()) || (strict ? ItemStack.isSameItemSameComponents(this.stack, stack.getBacking()) : this.stack.is(stack.getBacking().getItem()));
    }

    @Override
    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    @Override
    public Component getName() {
        return this.stack.getHoverName();
    }

    @Override
    public Class<ItemStack> getBackingClass() {
        return ItemStack.class;
    }

    @Override
    public ItemStack toItemStack(ServerPlayer player) {
        return this.stack.copy();
    }

    @Override
    public <E> @Nullable E get(DataComponentType<E> type) {
        return this.stack.get(type);
    }

    @Override
    public <E> E getOrDefault(DataComponentType<E> type, E fallback) {
        return this.stack.getOrDefault(type, fallback);
    }

    @Override
    public boolean contains(DataComponentType<?> type) {
        return this.stack.has(type);
    }

    @Override
    public ItemStack toDisplayItemStack(ServerPlayer player) {
        if (this.count == this.stack.getCount() && this.chance >= 1) {
            return this.stack.copy();
        } else {
            var lore = new ArrayList<Component>();
            try {
                lore.addAll(this.stack.getTooltipLines(Item.TooltipContext.of(player.level()), player, TooltipFlag.NORMAL));
                lore.removeFirst();
            } catch (Throwable e) {}

            Component extra;

            if (this.count > 99 && chance != 1) {
               extra = Component.translatable("text.polydex.item_stack.count_chance",
                       Component.literal("" + this.count).withStyle(ChatFormatting.WHITE),
                       Component.literal(PolydexPageUtils.formatChanceAmount(this.chance)).withStyle(ChatFormatting.WHITE)
               ).withStyle(ChatFormatting.YELLOW);
            } else if (this.count > 99) {
                extra = Component.translatable("text.polydex.item_stack.count",
                        Component.literal("" + this.count).withStyle(ChatFormatting.WHITE)
                ).withStyle(ChatFormatting.YELLOW);
            } else if (chance != 1) {
                extra = Component.translatable("text.polydex.item_stack.chance",
                        Component.literal(PolydexPageUtils.formatChanceAmount(this.chance)).withStyle(ChatFormatting.WHITE)
                ).withStyle(ChatFormatting.YELLOW);
            } else {
                extra = null;
            }

            if (extra != null) {
                lore.add(
                        Component.empty()
                                .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                                .append(extra)
                                .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))
                );
            }

            return GuiElementBuilder.from(this.stack)
                    .hideDefaultTooltip()
                    .setCount(this.count > 99 ? 1 : (int) this.count)
                    .setComponent(DataComponents.MAX_STACK_SIZE, 99)
                    .setLore(lore)
                    .asStack();
        }
    }

    @Override
    public @Nullable Identifier getId() {
        return BuiltInRegistries.ITEM.getKey(this.stack.getItem());
    }

    public Stream<TagKey<?>> streamTags() {
        //noinspection unchecked
        return (Stream<TagKey<?>>) (Object) this.stack.getItemHolder().tags();
    }

    @Override
    public ItemStack getBacking() {
        return this.stack;
    }

    @Override
    public int getSourceHashCode() {
        return System.identityHashCode(this.stack.getItem());
    }

    @Override
    public List<Component> getTexts(ServerPlayer player) {
        try {
            return this.stack.getTooltipLines(Item.TooltipContext.of(player.level()), player, TooltipFlag.NORMAL);
        } catch (Throwable e) {
            return List.of(this.getName());
        }
    }


    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PolydexItemStackImpl that = (PolydexItemStackImpl) object;
        return this.chance == that.chance && count == that.count && ItemStack.isSameItemSameComponents(stack, that.stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stackHash, chance, count);
    }
}
