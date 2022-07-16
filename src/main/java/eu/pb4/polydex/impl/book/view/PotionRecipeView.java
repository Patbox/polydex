package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.api.PolydexUiElements;
import eu.pb4.polydex.mixin.BrewingRecipeAccessor;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static eu.pb4.polydex.api.PolydexUtils.getIngredientDisplay;

public abstract class PotionRecipeView<T> implements ItemPageView<BrewingRecipeRegistry.Recipe<T>> {
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
    public GuiElement getIcon(ItemEntry entry, BrewingRecipeRegistry.Recipe<T> object, ServerPlayerEntity player, Runnable returnCallback) {
        return PolydexUiElements.POTION_RECIPE_ICON;
    }

    @Override
    public void renderLayer(ItemEntry entry, BrewingRecipeRegistry.Recipe<T> object, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        var access = (BrewingRecipeAccessor<T>) object;
        var base = toStack(entry, access.getInput());
        var out = toStack(entry, access.getOutput());

        layer.setSlot(12, getIngredientDisplay(access.getIngredient()));
        layer.setSlot(21, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).setName(Text.empty()));
        layer.setSlot(30, base);
        layer.setSlot(23, out);

    }

    protected abstract ItemStack toStack(ItemEntry entry, T object);
}
