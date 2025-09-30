package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.polydex.mixin.SingleStackRecipeAccessor;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;


public final class StonecuttingRecipePage extends AbstractRecipePolydexPage<StonecuttingRecipe> {
    public StonecuttingRecipePage(RecipeEntry<StonecuttingRecipe> recipe) {
        super(recipe);
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return InternalPageTextures.STONECUTTING;
    }

    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return PageIcons.STONECUTTING_RECIPE_ICON;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(2, 2, recipe.ingredient());
        if (!builder.hasTextures()) {
            builder.set(4, 2, new GuiElementBuilder(Items.ARROW).hideTooltip());
        }
        builder.setOutput(6, 2, getOutput(entry, player.getEntityWorld().getServer()));
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((SingleStackRecipeAccessor) this.recipe).getResult().copy();
    }

    @Override
    public boolean syncWithClient(ServerPlayerEntity player) {
        return false;
    }
}
