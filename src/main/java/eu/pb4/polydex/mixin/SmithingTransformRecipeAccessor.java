package eu.pb4.polydex.mixin;

import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformRecipeAccessor {
    @Accessor
    Ingredient getTemplate();

    @Accessor
    Ingredient getBase();

    @Accessor
    Ingredient getAddition();
}
