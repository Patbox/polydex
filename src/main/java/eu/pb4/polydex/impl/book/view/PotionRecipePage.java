package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.polydex.mixin.BrewingRecipeAccessor;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
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
    private final List<PolydexIngredient<ItemStack>> ingredient;
    protected final BrewingRecipeAccessor<T> access;

    public PotionRecipePage(Identifier identifier, BrewingRecipeRegistry.Recipe<T> recipe) {
        this.identifier = identifier;
        this.recipe = recipe;
        this.access = (BrewingRecipeAccessor<T>) recipe;
        this.ingredient = List.of(PolydexIngredient.of(((BrewingRecipeAccessor<T>) recipe).getIngredient()), this.customIngredient());
    }

    @Override
    public @Nullable Text getTexture(ServerPlayerEntity player) {
        return InternalPageTextures.POTION;
    }

    @Override
    public Identifier identifier() {
        return identifier;
    }

    @Override
    public List<PolydexIngredient<?>> getIngredients() {
        return (List<PolydexIngredient<?>>) this.identifier;
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.stack().getBackingClass() == ItemStack.class 
                && (this.access.getIngredient().test((ItemStack) entry.stack().getBacking())
                || this.isOwnerPotion((ItemStack) entry.stack().getBacking()));
    }

    protected abstract boolean isOwnerPotion(ItemStack backing);

    @Override
    public ItemStack getIcon(ServerPlayerEntity player) {
        return PageIcons.POTION_RECIPE_ICON;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        var base = getBaseStack(entry);
        var out = getOutStack(entry);

        builder.setIngredient(3, 1, access.getIngredient());
        if (!builder.hasTextures()) {
            builder.set(3, 2, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).setName(Text.empty()));
        }
        builder.set(3, 3, base);
        builder.setOutput(5, 2, out);

    }

    protected abstract ItemStack getBaseStack(PolydexEntry entry);

    protected abstract ItemStack getOutStack(PolydexEntry entry);

    protected abstract PolydexIngredient<ItemStack> customIngredient();

    public static class ItemBase extends PotionRecipePage<Item> {
        public ItemBase(Identifier identifier, BrewingRecipeRegistry.Recipe<Item> recipe) {
            super(identifier, recipe);
        }

        @Override
        protected boolean isOwnerPotion(ItemStack backing) {
            return backing.isOf(this.access.getOutput());
        }


        @Override
        protected ItemStack getBaseStack(PolydexEntry entry) {
            return PotionUtil.setPotion(this.access.getInput().getDefaultStack(), getPotion(entry));
        }

        @Override
        protected ItemStack getOutStack(PolydexEntry entry) {
            return PotionUtil.setPotion(this.access.getOutput().getDefaultStack(), getPotion(entry));
        }

        @Override
        protected PolydexIngredient<ItemStack> customIngredient() {
            return PolydexIngredient.of(Ingredient.ofItems(this.access.getInput()));
        }

        private Potion getPotion(PolydexEntry entry) {
            if (entry.stack().getBacking() instanceof ItemStack backing) {
                return PotionUtil.getPotion(backing);
            } else {
                return Potions.EMPTY;
            }
        }
    };

    public static class PotionBase extends PotionRecipePage<Potion> {
        public PotionBase(Identifier identifier, BrewingRecipeRegistry.Recipe<Potion> recipe) {
            super(identifier, recipe);
        }

        @Override
        protected boolean isOwnerPotion(ItemStack backing) {
            return PotionUtil.getPotion(backing) == this.access.getOutput() || (backing.isOf(Items.POTION) || backing.isOf(Items.SPLASH_POTION) ||backing.isOf(Items.LINGERING_POTION) || backing.isOf(Items.GLASS_BOTTLE));
        }

        @Override
        protected ItemStack getBaseStack(PolydexEntry entry) {
            return PotionUtil.setPotion(getStack(entry), this.access.getInput());
        }

        @Override
        protected ItemStack getOutStack(PolydexEntry entry) {
            return PotionUtil.setPotion(getStack(entry), this.access.getOutput());
        }

        @Override
        protected PolydexIngredient<ItemStack> customIngredient() {
            var stacks = new ArrayList<ItemStack>();
            for (var item : new Item[] { Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION }) {
                stacks.add(PotionUtil.setPotion(item.getDefaultStack(), this.access.getInput()));
            }

            return PolydexIngredient.of(Ingredient.ofStacks(stacks.stream()));
        }

        protected ItemStack getStack(PolydexEntry entry) {
            if (entry.stack().getBacking() instanceof ItemStack backing
                    && (backing.isOf(Items.POTION) || backing.isOf(Items.SPLASH_POTION) ||backing.isOf(Items.LINGERING_POTION) || backing.isOf(Items.GLASS_BOTTLE))) {

                return backing.getItem().getDefaultStack();
            } else {
                return Items.POTION.getDefaultStack();
            }

        }
    };
}
