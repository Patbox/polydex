package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.v1.recipe.PolydexCategory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;

public record RecipeTypePolydexCategory(Identifier identifier, RecipeType<?> type, Text name) implements PolydexCategory {
    private static final Map<RecipeType<?>, RecipeTypePolydexCategory> INSTANCES = new HashMap<>();

    public static PolydexCategory of(RecipeType<?> recipeType) {
        return INSTANCES.computeIfAbsent(recipeType, RecipeTypePolydexCategory::create);
    }

    private static RecipeTypePolydexCategory create(RecipeType<?> recipeType) {
        var id = Registries.RECIPE_TYPE.getId(recipeType);
        return new RecipeTypePolydexCategory(
                id.withPrefixedPath("recipe_type/"),
                recipeType,
                Text.translatable(Util.createTranslationKey("recipe_type", id))
        );
    }
}
