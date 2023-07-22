package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.api.v1.PolydexUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public abstract class SimpleRecipePolydexPage<T extends Recipe<?>> implements PolydexPage {
    public final T recipe;
    private final Identifier identifier;
    private final List<PolydexIngredient<?>> ingredients;

    public SimpleRecipePolydexPage(T recipe) {
        this.recipe = recipe;
        this.identifier = PolydexUtils.fromRecipe(recipe);
        //noinspection unchecked
        this.ingredients = (List<PolydexIngredient<?>>) (Object) recipe.getIngredients().stream().map(PolydexIngredient::of).toList();
    }

    @Override
    public Identifier identifier() {
        return this.identifier;
    }

    @Override
    public List<PolydexIngredient<?>> getIngredients() {
        return this.ingredients;
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        var out = this.recipe.getOutput(server.getRegistryManager());
        return entry.isPartOf(PolydexStack.of(out));
    }
}
