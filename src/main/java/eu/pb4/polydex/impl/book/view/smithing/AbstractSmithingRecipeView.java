package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;


public abstract class AbstractSmithingRecipeView<T extends SmithingRecipe> extends AbstractRecipePolydexPage<T> {
    private final List<PolydexIngredient<?>> ingrendients;

    public AbstractSmithingRecipeView(RecipeHolder<T> recipe) {
        super(recipe);
        this.ingrendients = List.of(PolydexIngredient.of(getBase()), PolydexIngredient.of(getTemplate()), PolydexIngredient.of(getAddition()));
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return InternalPageTextures.SMITHING;
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return PageIcons.SMITING_RECIPE_ICON;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder builder) {
        builder.setIngredient(2, 2, this.getTemplate());
        builder.setIngredient(3, 2, this.getBaseItem(entry));
        builder.setIngredient(4, 2, this.getAddition());
        builder.setOutput(6, 2, this.getOutput(entry, Objects.requireNonNull(player.level().getServer())));
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return this.ingrendients;
    }

    protected SlotDisplay getBaseItem(@Nullable PolydexEntry entry) {
        return getBase().display();
    }

    protected abstract Ingredient getTemplate();
    protected abstract Ingredient getAddition();
    protected abstract Ingredient getBase();
}
