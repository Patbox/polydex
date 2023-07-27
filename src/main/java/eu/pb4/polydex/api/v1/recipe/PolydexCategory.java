package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.book.GenericPolydexCategory;
import eu.pb4.polydex.impl.book.RecipeTypePolydexCategory;
import net.minecraft.recipe.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public interface PolydexCategory {
    PolydexCategory CRAFTING = of(RecipeType.CRAFTING);
    PolydexCategory SMELTING = of(RecipeType.SMELTING);
    PolydexCategory BLASTING = of(RecipeType.BLASTING);
    PolydexCategory SMOKING = of(RecipeType.SMOKING);
    PolydexCategory CAMPFIRE_COOKING = of(RecipeType.CAMPFIRE_COOKING);
    PolydexCategory STONECUTTING = of(RecipeType.STONECUTTING);
    PolydexCategory SMITHING = of(RecipeType.SMITHING);
    PolydexCategory BREWING = of(new Identifier("brewing"));
    PolydexCategory CUSTOM = of(new Identifier("polydex:custom"));


    Identifier identifier();
    Text name();

    static PolydexCategory of(RecipeType<?> recipeType) {
        return RecipeTypePolydexCategory.of(recipeType);
    }

    static PolydexCategory of(Identifier identifier) {
        return GenericPolydexCategory.of(identifier);
    }
}
