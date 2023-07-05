package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.api.PageView;
import eu.pb4.polydex.api.recipe.PageBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public final class AbstractCookingRecipeView<T extends AbstractCookingRecipe> implements PageView<T> {
    private final ItemStack icon;

    public AbstractCookingRecipeView(Item icon) {
        this.icon = icon.getDefaultStack();
    }


    @Override
    public ItemStack getIcon(ItemEntry entry, T object, ServerPlayerEntity player) {
        return this.icon;
    }

    @Override
    public void createPage(ItemEntry entry, T recipe, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(2, 2,recipe.getIngredients().get(0));
        builder.set(4, 2, new GuiElementBuilder(Items.BLAZE_POWDER)
                .setName(Text.translatable("text.polydex.view.cooking_time", Text.literal("" + (recipe.getCookTime() / 20d) + "s")
                        .formatted(Formatting.WHITE)).formatted(Formatting.GOLD)));
        if (recipe.getExperience() != 0) {
            builder.set(4, 3, new GuiElementBuilder(Items.EXPERIENCE_BOTTLE)
                    .setName(Text.translatable("text.polydex.view.experience", Text.literal("" + recipe.getExperience())
                            .append(Text.translatable("text.polydex.view.experience.points")).formatted(Formatting.WHITE)).formatted(Formatting.GREEN)));
        }
        builder.setOutput(6, 2, recipe.getOutput(player.server.getRegistryManager()));
    }
}
