package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;


public final class StonecuttingRecipePage extends SimpleRecipePolydexPage<StonecuttingRecipe> {
    public StonecuttingRecipePage(StonecuttingRecipe recipe) {
        super(recipe);
    }

    @Override
    public @Nullable Text getTexture(ServerPlayerEntity player) {
        return InternalPageTextures.STONECUTTING;
    }

    @Override
    public ItemStack getIcon(ServerPlayerEntity player) {
        return PageIcons.STONECUTTING_RECIPE_ICON;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(2, 2, recipe.getIngredients().get(0));
        //builder.set(4, 2, new GuiElementBuilder(Items.ARROW).setName(Text.empty()));
        builder.setOutput(6, 2, recipe.getOutput(player.server.getRegistryManager()));
    }
}
