package eu.pb4.polydex.impl.book;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
        this.stackHash = ItemStack.hashCode(stack);
    }

    public PolydexItemStackImpl(Item item) {
        this.stack = new ItemStack(item);
        this.chance = 1;
        this.count = 1;
        this.stackHash = ItemStack.hashCode(stack);
    }

    public static PolydexStack<ItemStack> of(ItemStack stack, long count, float chance) {
        if (count == 1 && chance == 1f && stack.getComponentChanges().isEmpty()) {
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
        return (this.isEmpty() && stack.isEmpty()) || (strict ? ItemStack.areItemsAndComponentsEqual(this.stack, stack.getBacking()) : this.stack.isOf(stack.getBacking().getItem()));
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
    public <E> @Nullable E get(ComponentType<E> type) {
        return this.stack.get(type);
    }

    @Override
    public <E> E getOrDefault(ComponentType<E> type, E fallback) {
        return this.stack.getOrDefault(type, fallback);
    }

    @Override
    public boolean contains(ComponentType<?> type) {
        return this.stack.contains(type);
    }

    @Override
    public ItemStack toDisplayItemStack(ServerPlayerEntity player) {
        if (this.count == this.stack.getCount() && this.chance >= 1) {
            return this.stack.copy();
        } else {
            var lore = new ArrayList<Text>();
            try {
                lore.addAll(this.stack.getTooltip(Item.TooltipContext.create(player.getWorld()), player, TooltipType.BASIC));
                lore.remove(0);
            } catch (Throwable e) {}

            Text extra;

            if (this.count > 99 && chance != 1) {
               extra = Text.translatable("text.polydex.item_stack.count_chance",
                       Text.literal("" + this.count).formatted(Formatting.WHITE),
                       Text.literal(PolydexPageUtils.formatChanceAmount(this.chance)).formatted(Formatting.WHITE)
               ).formatted(Formatting.YELLOW);
            } else if (this.count > 99) {
                extra = Text.translatable("text.polydex.item_stack.count",
                        Text.literal("" + this.count).formatted(Formatting.WHITE)
                ).formatted(Formatting.YELLOW);
            } else if (chance != 1) {
                extra = Text.translatable("text.polydex.item_stack.chance",
                        Text.literal(PolydexPageUtils.formatChanceAmount(this.chance)).formatted(Formatting.WHITE)
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
                    .hideDefaultTooltip()
                    .setCount(this.count > 99 ? 1 : (int) this.count)
                    .setComponent(DataComponentTypes.MAX_STACK_SIZE, 99)
                    .setLore(lore)
                    .asStack();
        }
    }

    @Override
    public @Nullable Identifier getId() {
        return Registries.ITEM.getId(this.stack.getItem());
    }

    public Stream<TagKey<?>> streamTags() {
        //noinspection unchecked
        return (Stream<TagKey<?>>) (Object) this.stack.getRegistryEntry().streamTags();
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
    public List<Text> getTexts(ServerPlayerEntity player) {
        try {
            return this.stack.getTooltip(Item.TooltipContext.create(player.getWorld()), player, TooltipType.BASIC);
        } catch (Throwable e) {
            return List.of(this.getName());
        }
    }


    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PolydexItemStackImpl that = (PolydexItemStackImpl) object;
        return this.chance == that.chance && count == that.count && ItemStack.areItemsAndComponentsEqual(stack, that.stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stackHash, chance, count);
    }
}
