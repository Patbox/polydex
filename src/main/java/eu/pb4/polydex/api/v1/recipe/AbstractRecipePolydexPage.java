package eu.pb4.polydex.api.v1.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public abstract class AbstractRecipePolydexPage<T extends Recipe<?>> implements PolydexPage {
    protected final T recipe;
    private final Identifier identifier;
    protected final Identifier recipeId;
    private final List<PolydexIngredient<?>> ingredients;
    private @Nullable PolydexStack<?> resultStackCache = null;

    public AbstractRecipePolydexPage(RecipeEntry<T> recipe) {
        this.recipe = recipe.value();
        this.recipeId = recipe.id();
        this.identifier = PolydexPageUtils.identifierFromRecipe(recipe.id());
        //noinspection unchecked
        this.ingredients = (List<PolydexIngredient<?>>) (Object) recipe.value().getIngredients().stream().map(PolydexIngredient::of).toList();
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return this.recipe.getResult(player.server.getRegistryManager());
    }

    @Override
    public String getGroup() {
        return this.recipe.getGroup();
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
        if (resultStackCache == null) {
            var out = this.recipe.getResult(server.getRegistryManager());
            resultStackCache = PolydexStack.of(out);
        }
        return entry.isPartOf(resultStackCache);
    }
}
