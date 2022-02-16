package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import static eu.pb4.polydex.api.PolydexUtils.getIngredientDisplay;

public final class AbstractCookingRecipeView<T extends AbstractCookingRecipe> implements ItemPageView<T> {
    private final ItemStack icon;

    public AbstractCookingRecipeView(Item icon) {
        this.icon = icon.getDefaultStack();
    }

    @Override
    public GuiElement getIcon(ItemEntry entry, T recipe, ServerPlayerEntity player, Runnable returnCallback) {
        return new GuiElement(this.icon, GuiElement.EMPTY_CALLBACK);
    }

    @Override
    public void renderLayer(ItemEntry entry, T recipe, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        layer.setSlot(20, getIngredientDisplay(recipe.getIngredients().get(0)));
        layer.setSlot(22, new GuiElementBuilder(Items.BLAZE_ROD).setName(new TranslatableText("name.polydex.view.cooking_time", new LiteralText("" +  (recipe.getCookTime() / 20d) + "s").formatted(Formatting.WHITE)).formatted(Formatting.GOLD)));
        layer.setSlot(31, new GuiElementBuilder(Items.EXPERIENCE_BOTTLE).setName(new TranslatableText("name.polydex.view.experience", new LiteralText("" +  recipe.getExperience()).append(new TranslatableText("name.polydex.view.experience.points")).formatted(Formatting.WHITE)).formatted(Formatting.GREEN)));
        layer.setSlot(24, recipe.getOutput());
    }
}
