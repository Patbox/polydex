package eu.pb4.polydex.api.v1.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public abstract class AbstractRecipePolydexPage<T extends Recipe<?>> implements PolydexPage {
    public final T recipe;
    private final Identifier identifier;
    private final List<PolydexIngredient<?>> ingredients;

    public AbstractRecipePolydexPage(T recipe) {
        this.recipe = recipe;
        this.identifier = PolydexPageUtils.identifierFromRecipe(recipe);
        //noinspection unchecked
        this.ingredients = (List<PolydexIngredient<?>>) (Object) recipe.getIngredients().stream().map(PolydexIngredient::of).toList();
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return this.recipe.getOutput(player.server.getRegistryManager());
    }

    @Override
    public String sortingId() {
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
        var out = this.recipe.getOutput(server.getRegistryManager());
        return entry.isPartOf(PolydexStack.of(out));
    }
}
