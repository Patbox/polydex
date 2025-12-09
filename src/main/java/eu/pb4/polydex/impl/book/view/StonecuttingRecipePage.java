package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.polydex.mixin.SingleItemRecipeAccessor;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.jetbrains.annotations.Nullable;


public final class StonecuttingRecipePage extends AbstractRecipePolydexPage<StonecutterRecipe> {
    public StonecuttingRecipePage(RecipeHolder<StonecutterRecipe> recipe) {
        super(recipe);
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return InternalPageTextures.STONECUTTING;
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return PageIcons.STONECUTTING_RECIPE_ICON;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder builder) {
        builder.setIngredient(2, 2, recipe.input());
        if (!builder.hasTextures()) {
            builder.set(4, 2, new GuiElementBuilder(Items.ARROW).hideTooltip());
        }
        builder.setOutput(6, 2, getOutput(entry, player.level().getServer()));
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((SingleItemRecipeAccessor) this.recipe).getResult().copy();
    }

    @Override
    public boolean syncWithClient(ServerPlayer player) {
        return false;
    }
}
