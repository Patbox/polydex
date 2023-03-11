package eu.pb4.polydex.mixin;

import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.LegacySmithingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LegacySmithingRecipe.class)
public interface LegacySmithingRecipeAccessor {
    @Accessor
    Ingredient getBase();

    @Accessor
    Ingredient getAddition();
}
