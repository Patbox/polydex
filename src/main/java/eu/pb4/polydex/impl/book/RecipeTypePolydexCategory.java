package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.v1.recipe.PolydexCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.HashMap;
import java.util.Map;

public record RecipeTypePolydexCategory(Identifier identifier, RecipeType<?> type, Component name) implements PolydexCategory {
    private static final Map<RecipeType<?>, RecipeTypePolydexCategory> INSTANCES = new HashMap<>();

    public static PolydexCategory of(RecipeType<?> recipeType) {
        return INSTANCES.computeIfAbsent(recipeType, RecipeTypePolydexCategory::create);
    }

    private static RecipeTypePolydexCategory create(RecipeType<?> recipeType) {
        var id = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
        return new RecipeTypePolydexCategory(
                id.withPrefix("recipe_type/"),
                recipeType,
                Component.translatable(Util.makeDescriptionId("recipe_type", id))
        );
    }
}
