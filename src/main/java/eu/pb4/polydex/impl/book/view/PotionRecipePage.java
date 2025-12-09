package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class PotionRecipePage<T> implements PolydexPage {
    private final Identifier identifier;
    protected final PotionBrewing.Mix<T> recipe;
    private final List<PolydexIngredient<?>> ingredient;

    public PotionRecipePage(Identifier identifier, PotionBrewing.Mix<T> recipe) {
        this.identifier = identifier;
        this.recipe = recipe;
        this.ingredient = List.of(PolydexIngredient.of(recipe.ingredient()), this.customIngredient());
    }

    @Override
    public boolean syncWithClient(ServerPlayer player) {
        return false;
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return InternalPageTextures.POTION;
    }

    @Override
    public Identifier identifier() {
        return identifier;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return this.ingredient;
    }

    @Override
    public List<PolydexCategory> categories() {
        return List.of(PolydexCategory.BREWING);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.stack().getBackingClass() == ItemStack.class 
                && (this.isOwnerPotion((ItemStack) entry.stack().getBacking()));
    }

    protected abstract boolean isOwnerPotion(ItemStack backing);

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return PageIcons.POTION_RECIPE_ICON;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder builder) {
        var base = getBaseStack(entry);
        var out = getOutStack(entry);

        builder.setIngredient(3, 1, this.recipe.ingredient());
        if (!builder.hasTextures()) {
            builder.set(3, 2, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).hideTooltip());
        }
        builder.setIngredient(3, 3, base);
        builder.setOutput(5, 2, out);

    }

    protected abstract ItemStack getBaseStack(@Nullable PolydexEntry entry);

    protected abstract ItemStack getOutStack(@Nullable  PolydexEntry entry);

    protected abstract PolydexIngredient<ItemStack> customIngredient();

    public static class ItemBase extends PotionRecipePage<Item> {
        public ItemBase(Identifier identifier, PotionBrewing.Mix<Item> recipe) {
            super(identifier, recipe);
        }

        @Override
        protected boolean isOwnerPotion(ItemStack backing) {
            return backing.is(this.recipe.to().value());
        }


        @Override
        protected ItemStack getBaseStack(@Nullable PolydexEntry entry) {
            var input = this.recipe.from().value().getDefaultInstance();
            input.set(DataComponents.POTION_CONTENTS, getPotion(entry));
            return input;
        }

        @Override
        protected ItemStack getOutStack(@Nullable PolydexEntry entry) {
            var input = this.recipe.to().value().getDefaultInstance();
            input.set(DataComponents.POTION_CONTENTS, getPotion(entry));
            return input;
        }

        @Override
        protected PolydexIngredient<ItemStack> customIngredient() {
            return PolydexIngredient.of(Ingredient.of(this.recipe.from().value()));
        }

        private PotionContents getPotion(@Nullable PolydexEntry entry) {
            if (entry != null && entry.stack().getBacking() instanceof ItemStack backing) {
                return backing.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            } else {
                return PotionContents.EMPTY;
            }
        }

        @Override
        public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
            return getOutStack(entry);
        }
    };

    public static class PotionBase extends PotionRecipePage<Potion> {
        public PotionBase(Identifier identifier, PotionBrewing.Mix<Potion> recipe) {
            super(identifier, recipe);
        }

        @Override
        protected boolean isOwnerPotion(ItemStack backing) {
            var x = backing.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

            return (x.potion().map(Holder::value).orElse(null) == this.recipe.to().value())
                    && ((backing.is(Items.POTION) || backing.is(Items.SPLASH_POTION) ||backing.is(Items.LINGERING_POTION) || backing.is(Items.GLASS_BOTTLE)));
        }

        @Override
        protected ItemStack getBaseStack(PolydexEntry entry) {
            var x = getStack(entry);
            x.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withPotion(this.recipe.from()));
            return x;
        }

        @Override
        protected ItemStack getOutStack(@Nullable  PolydexEntry entry) {
            var x = getStack(entry);
            x.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withPotion(this.recipe.to()));
            return x;        }

        @Override
        protected PolydexIngredient<ItemStack> customIngredient() {
            return PotionIngredient.of(this.recipe.from());
        }

        @Override
        public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
            return getOutStack(entry);
        }

        private record PotionIngredient(Holder<Potion> potion, List<PolydexStack<ItemStack>> stacks) implements PolydexIngredient<ItemStack> {


            public static PolydexIngredient<ItemStack> of(Holder<Potion> input) {
                var list = new ArrayList<PolydexStack<ItemStack>>();
                for (var x : new Item[] { Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION }) {
                    var y = x.getDefaultInstance();
                    y.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withPotion(input));
                    list.add(PolydexStack.of(y));
                }
                return new PotionIngredient(input, list);
            }

            @Override
            public List<PolydexStack<ItemStack>> asStacks() {
                return stacks;
            }

            @Override
            public float chance() {
                return 1;
            }

            @Override
            public long amount() {
                return 1;
            }

            @Override
            public boolean matchesDirect(PolydexStack<ItemStack> stack, boolean strict) {
                var potion = stack.getBacking().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                return this.potion.value() == potion.potion().map(Holder::value).orElse(null) && correctBase(stack.getBacking());
            }

            private boolean correctBase(ItemStack backing) {
                return backing.is(Items.POTION) || backing.is(Items.SPLASH_POTION) || backing.is(Items.LINGERING_POTION);
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Class<ItemStack> getBackingClass() {
                return ItemStack.class;
            }
        }

        protected ItemStack getStack(@Nullable  PolydexEntry entry) {
            if (entry != null && entry.stack().getBacking() instanceof ItemStack backing
                    && (backing.is(Items.POTION) || backing.is(Items.SPLASH_POTION) ||backing.is(Items.LINGERING_POTION) || backing.is(Items.GLASS_BOTTLE))) {

                return backing.getItem().getDefaultInstance();
            } else {
                return Items.POTION.getDefaultInstance();
            }

        }
    };
}
