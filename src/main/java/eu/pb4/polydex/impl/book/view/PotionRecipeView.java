package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.api.PageView;
import eu.pb4.polydex.api.recipe.PageBuilder;
import eu.pb4.polydex.api.recipe.PageIcons;
import eu.pb4.polydex.mixin.BrewingRecipeAccessor;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public abstract class PotionRecipeView<T> implements PageView<BrewingRecipeRegistry.Recipe<T>> {
    public static PotionRecipeView<Item> ITEM = new PotionRecipeView<>() {
        @Override
        protected ItemStack toStack(ItemEntry entry, Item object) {
            var potion = PotionUtil.getPotion(entry.stack());

            return PotionUtil.setPotion(object.getDefaultStack(), potion);
        }
    };

    public static PotionRecipeView<Potion> POTION = new PotionRecipeView<>() {
        @Override
        protected ItemStack toStack(ItemEntry entry, Potion object) {
            return PotionUtil.setPotion(entry.item().getDefaultStack(), object);
        }
    };


    @Override
    public ItemStack getIcon(ItemEntry entry, BrewingRecipeRegistry.Recipe<T> object, ServerPlayerEntity player) {
        return PageIcons.POTION_RECIPE_ICON;
    }

    @Override
    public void createPage(ItemEntry entry, BrewingRecipeRegistry.Recipe<T> object, ServerPlayerEntity player, PageBuilder builder) {
        var access = (BrewingRecipeAccessor<T>) object;
        var base = toStack(entry, access.getInput());
        var out = toStack(entry, access.getOutput());

        builder.setIngredient(3, 1, access.getIngredient());
        builder.set(3, 2, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).setName(Text.empty()));
        builder.set(3, 3, base);
        builder.setOutput(5, 2, out);

    }

    protected abstract ItemStack toStack(ItemEntry entry, T object);
}
