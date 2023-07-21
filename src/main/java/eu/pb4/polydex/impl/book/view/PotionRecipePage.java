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
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PotionRecipePage<T> implements PolydexPage {
    private final Identifier identifier;
    protected final BrewingRecipeRegistry.Recipe<T> recipe;
    private final PolydexIngredient<ItemStack> ingredient;
    protected final BrewingRecipeAccessor<T> access;

    public PotionRecipePage(Identifier identifier, BrewingRecipeRegistry.Recipe<T> recipe) {
        this.identifier = identifier;
        this.recipe = recipe;
        this.access = (BrewingRecipeAccessor<T>) recipe;
        this.ingredient = PolydexIngredient.of(((BrewingRecipeAccessor<T>) recipe).getIngredient());
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
        return List.of(this.ingredient);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.stack().getBackingClass() == ItemStack.class;
    }

    @Override
    public ItemStack getIcon(ServerPlayerEntity player) {
        return PageIcons.POTION_RECIPE_ICON;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        var base = toStack(entry);
        var out = toStack(entry);

        builder.setIngredient(3, 1, access.getIngredient());
        builder.set(3, 2, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).setName(Text.empty()));
        builder.set(3, 3, base);
        builder.setOutput(5, 2, out);

    }

    protected abstract ItemStack toStack(PolydexEntry entry);


    public static class ItemBase extends PotionRecipePage<Item> {
        public ItemBase(Identifier identifier, BrewingRecipeRegistry.Recipe<Item> recipe) {
            super(identifier, recipe);
        }

        @Override
        protected ItemStack toStack(PolydexEntry entry) {
            var potion = PotionUtil.getPotion((ItemStack) entry.stack().getBacking());

            return PotionUtil.setPotion(this.access.getOutput().getDefaultStack(), potion);
        }
    };

    public static class PotionBase extends PotionRecipePage<Potion> {
        public PotionBase(Identifier identifier, BrewingRecipeRegistry.Recipe<Potion> recipe) {
            super(identifier, recipe);
        }

        @Override
        protected ItemStack toStack(PolydexEntry entry) {
            return PotionUtil.setPotion(((ItemStack) entry.stack().getBacking()).getItem().getDefaultStack(), this.access.getOutput());
        }
    };
}
