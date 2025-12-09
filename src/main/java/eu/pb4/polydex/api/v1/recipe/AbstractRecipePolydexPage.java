package eu.pb4.polydex.api.v1.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public abstract class AbstractRecipePolydexPage<T extends Recipe<?>> implements PolydexPage {
    protected final T recipe;
    private final Identifier identifier;
    protected final ResourceKey<Recipe<?>> recipeId;
    private final List<PolydexIngredient<?>> ingredients;

    public AbstractRecipePolydexPage(RecipeHolder<T> recipe) {
        this.recipe = recipe.value();
        this.recipeId = recipe.id();
        this.identifier = PolydexPageUtils.identifierFromRecipe(recipe.id().identifier());
        //noinspection unchecked
        this.ingredients = (List<PolydexIngredient<?>>) (Object) recipe.value().placementInfo().ingredients().stream().map(PolydexIngredient::of).toList();
    }

    public abstract ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server);

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        return getOutput(entry, player.level().getServer());
    }

    @Override
    public String getGroup() {
        return this.recipe.group();
    }

    @Override
    public Identifier identifier() {
        return this.identifier;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return this.ingredients;
    }

    @Override
    public List<PolydexCategory> categories() {
        return List.of(PolydexCategory.of(this.recipe.getType()));
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        var out = getOutput(entry, server);
        return entry.isPartOf(PolydexStack.of(out));
    }
}
