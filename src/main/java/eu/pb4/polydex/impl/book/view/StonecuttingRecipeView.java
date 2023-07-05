package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.api.PageView;
import eu.pb4.polydex.api.recipe.PageBuilder;
import eu.pb4.polydex.api.recipe.PageIcons;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;


public final class StonecuttingRecipeView implements PageView<StonecuttingRecipe> {
    @Override
    public ItemStack getIcon(ItemEntry entry, StonecuttingRecipe recipe, ServerPlayerEntity player) {
        return PageIcons.STONECUTTING_RECIPE_ICON;
    }

    @Override
    public void createPage(ItemEntry entry, StonecuttingRecipe recipe, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(2, 2, recipe.getIngredients().get(0));
        builder.set(4, 2, new GuiElementBuilder(Items.ARROW).setName(Text.empty()));
        builder.setOutput(5, 2, recipe.getOutput(player.server.getRegistryManager()));
    }
}
