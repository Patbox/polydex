package eu.pb4.polydex.mixin;

import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SmithingTrimRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingTrimRecipe.class)
public interface SmithingTrimRecipeAccessor {
    @Accessor
    Ingredient getTemplate();

    @Accessor
    Ingredient getBase();

    @Accessor
    Ingredient getAddition();
}
