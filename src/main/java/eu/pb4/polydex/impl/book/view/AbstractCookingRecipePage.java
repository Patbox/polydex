package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.AbstractRecipePolydexPage;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.polydex.impl.book.ui.GuiUtils;
import eu.pb4.polydex.mixin.SingleStackRecipeAccessor;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;


public final class AbstractCookingRecipePage<T extends AbstractCookingRecipe> extends AbstractRecipePolydexPage<T> {
    private final ItemStack icon;
    private final boolean sync;

    public AbstractCookingRecipePage(RecipeEntry<T> recipe, Item icon, boolean sync) {
        super(recipe);
        this.icon = icon.getDefaultStack();
        this.sync = sync;
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return InternalPageTextures.SMELTING;
    }

    public static <T extends AbstractCookingRecipe> Function<RecipeEntry<T>, PolydexPage> of(Item icon) {
        return (r) -> new AbstractCookingRecipePage<T>(r, icon, false);
    }

    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return this.icon;
    }

    @Override
    public boolean syncWithClient(ServerPlayerEntity player) {
        return this.sync;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayerEntity player, PageBuilder builder) {
        builder.setIngredient(3, 2, recipe.ingredient().toDisplay());
        builder.set(3, 3, GuiUtils.flame(player)
                .setName(Text.translatable("text.polydex.view.cooking_time", Text.literal("" + (recipe.getCookingTime() / 20d) + "s")
                        .formatted(Formatting.WHITE)).formatted(Formatting.GOLD)));
        if (recipe.getExperience() != 0) {
            builder.set(5, 3, GuiUtils.xp(player)
                    .setName(Text.translatable("text.polydex.view.experience", Text.literal("" + recipe.getExperience())
                            .append(Text.translatable("text.polydex.view.experience.points")).formatted(Formatting.WHITE)).formatted(Formatting.GREEN)));
        }
        builder.setOutput(5, 2, getOutput(entry, player.getEntityWorld().getServer()));
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((SingleStackRecipeAccessor) this.recipe).getResult().copy();
    }
}
