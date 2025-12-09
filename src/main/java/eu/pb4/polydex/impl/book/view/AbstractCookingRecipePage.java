package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.AbstractRecipePolydexPage;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.polydex.impl.book.ui.GuiUtils;
import eu.pb4.polydex.mixin.SingleItemRecipeAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;


public final class AbstractCookingRecipePage<T extends AbstractCookingRecipe> extends AbstractRecipePolydexPage<T> {
    private final ItemStack icon;
    private final boolean sync;

    public AbstractCookingRecipePage(RecipeHolder<T> recipe, Item icon, boolean sync) {
        super(recipe);
        this.icon = icon.getDefaultInstance();
        this.sync = sync;
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return InternalPageTextures.SMELTING;
    }

    public static <T extends AbstractCookingRecipe> Function<RecipeHolder<T>, PolydexPage> of(Item icon) {
        return (r) -> new AbstractCookingRecipePage<T>(r, icon, false);
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return this.icon;
    }

    @Override
    public boolean syncWithClient(ServerPlayer player) {
        return this.sync;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayer player, PageBuilder builder) {
        builder.setIngredient(3, 2, recipe.input().display());
        builder.set(3, 3, GuiUtils.flame(player)
                .setName(Component.translatable("text.polydex.view.cooking_time", Component.literal("" + (recipe.cookingTime() / 20d) + "s")
                        .withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GOLD)));
        if (recipe.experience() != 0) {
            builder.set(5, 3, GuiUtils.xp(player)
                    .setName(Component.translatable("text.polydex.view.experience", Component.literal("" + recipe.experience())
                            .append(Component.translatable("text.polydex.view.experience.points")).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GREEN)));
        }
        builder.setOutput(5, 2, getOutput(entry, player.level().getServer()));
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return ((SingleItemRecipeAccessor) this.recipe).getResult().copy();
    }
}
