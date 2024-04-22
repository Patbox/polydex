package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class PotionRecipePage<T> implements PolydexPage {
    private final Identifier identifier;
    protected final BrewingRecipeRegistry.Recipe<T> recipe;
    private final List<PolydexIngredient<?>> ingredient;

    public PotionRecipePage(Identifier identifier, BrewingRecipeRegistry.Recipe<T> recipe) {
        this.identifier = identifier;
        this.recipe = recipe;
        this.ingredient = List.of(PolydexIngredient.of(recipe.ingredient()), this.customIngredient());
    }

    @Override
    public boolean syncWithClient(ServerPlayerEntity player) {
        return false;
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
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
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return PageIcons.POTION_RECIPE_ICON;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        var base = getBaseStack(entry);
        var out = getOutStack(entry);

        builder.setIngredient(3, 1, this.recipe.ingredient());
        if (!builder.hasTextures()) {
            builder.set(3, 2, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).setName(Text.empty()));
        }
        builder.set(3, 3, base);
        builder.setOutput(5, 2, out);

    }

    protected abstract ItemStack getBaseStack(@Nullable PolydexEntry entry);

    protected abstract ItemStack getOutStack(@Nullable  PolydexEntry entry);

    protected abstract PolydexIngredient<ItemStack> customIngredient();

    public static class ItemBase extends PotionRecipePage<Item> {
        public ItemBase(Identifier identifier, BrewingRecipeRegistry.Recipe<Item> recipe) {
            super(identifier, recipe);
        }

        @Override
        protected boolean isOwnerPotion(ItemStack backing) {
            return backing.isOf(this.recipe.to().value());
        }


        @Override
        protected ItemStack getBaseStack(@Nullable PolydexEntry entry) {
            var input = this.recipe.from().value().getDefaultStack();
            input.set(DataComponentTypes.POTION_CONTENTS, getPotion(entry));
            return input;
        }

        @Override
        protected ItemStack getOutStack(@Nullable PolydexEntry entry) {
            var input = this.recipe.to().value().getDefaultStack();
            input.set(DataComponentTypes.POTION_CONTENTS, getPotion(entry));
            return input;
        }

        @Override
        protected PolydexIngredient<ItemStack> customIngredient() {
            return PolydexIngredient.of(Ingredient.ofItems(this.recipe.from().value()));
        }

        private PotionContentsComponent getPotion(@Nullable PolydexEntry entry) {
            if (entry != null && entry.stack().getBacking() instanceof ItemStack backing) {
                return backing.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            } else {
                return PotionContentsComponent.DEFAULT;
            }
        }

        @Override
        public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
            return getOutStack(entry);
        }
    };

    public static class PotionBase extends PotionRecipePage<Potion> {
        public PotionBase(Identifier identifier, BrewingRecipeRegistry.Recipe<Potion> recipe) {
            super(identifier, recipe);
        }

        @Override
        protected boolean isOwnerPotion(ItemStack backing) {
            var x = backing.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            return (x.potion().map(RegistryEntry::value).orElse(null) == this.recipe.to().value())
                    && ((backing.isOf(Items.POTION) || backing.isOf(Items.SPLASH_POTION) ||backing.isOf(Items.LINGERING_POTION) || backing.isOf(Items.GLASS_BOTTLE)));
        }

        @Override
        protected ItemStack getBaseStack(PolydexEntry entry) {
            var x = getStack(entry);
            x.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(this.recipe.from()));
            return x;
        }

        @Override
        protected ItemStack getOutStack(@Nullable  PolydexEntry entry) {
            var x = getStack(entry);
            x.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(this.recipe.to()));
            return x;        }

        @Override
        protected PolydexIngredient<ItemStack> customIngredient() {
            return PotionIngredient.of(this.recipe.from());
        }

        @Override
        public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
            return getOutStack(entry);
        }

        private record PotionIngredient(RegistryEntry<Potion> potion, List<PolydexStack<ItemStack>> stacks) implements PolydexIngredient<ItemStack> {


            public static PolydexIngredient<ItemStack> of(RegistryEntry<Potion> input) {
                var list = new ArrayList<PolydexStack<ItemStack>>();
                for (var x : new Item[] { Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION }) {
                    var y = x.getDefaultStack();
                    y.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(input));
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
                var potion = stack.getBacking().getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
                return this.potion.value() == potion.potion().map(RegistryEntry::value).orElse(null) && correctBase(stack.getBacking());
            }

            private boolean correctBase(ItemStack backing) {
                return backing.isOf(Items.POTION) || backing.isOf(Items.SPLASH_POTION) || backing.isOf(Items.LINGERING_POTION);
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
                    && (backing.isOf(Items.POTION) || backing.isOf(Items.SPLASH_POTION) ||backing.isOf(Items.LINGERING_POTION) || backing.isOf(Items.GLASS_BOTTLE))) {

                return backing.getItem().getDefaultStack();
            } else {
                return Items.POTION.getDefaultStack();
            }

        }
    };
}
