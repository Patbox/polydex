package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.SimpleRecipePolydexPage;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;


public final class AbstractCookingRecipePage<T extends AbstractCookingRecipe> extends SimpleRecipePolydexPage<T> {
    private final ItemStack icon;

    public AbstractCookingRecipePage(T recipe, Item icon) {
        super(recipe);
        this.icon = icon.getDefaultStack();
    }

    @Override
    public @Nullable Text getTexture(ServerPlayerEntity player) {
        return InternalPageTextures.SMELTING;
    }

    public static <T extends AbstractCookingRecipe> Function<T, PolydexPage> of(Item icon) {
        return (r) -> new AbstractCookingRecipePage<T>(r, icon);
    }

    @Override
    public ItemStack getIcon(ServerPlayerEntity player) {
        return this.icon;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(3, 2, recipe.getIngredients().get(0));
        builder.set(3, 3, new GuiElementBuilder(Items.BLAZE_POWDER)
                .setName(Text.translatable("text.polydex.view.cooking_time", Text.literal("" + (recipe.getCookTime() / 20d) + "s")
                        .formatted(Formatting.WHITE)).formatted(Formatting.GOLD)));
        if (recipe.getExperience() != 0) {
            builder.set(5, 3, new GuiElementBuilder(Items.EXPERIENCE_BOTTLE)
                    .setName(Text.translatable("text.polydex.view.experience", Text.literal("" + recipe.getExperience())
                            .append(Text.translatable("text.polydex.view.experience.points")).formatted(Formatting.WHITE)).formatted(Formatting.GREEN)));
        }
        builder.setOutput(5, 2, recipe.getOutput(player.server.getRegistryManager()));
    }
}
