package eu.pb4.polydex.mixin;

import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SmithingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingRecipe.class)
public interface SmithingRecipeAccessor {
    @Accessor
    Ingredient getBase();

    @Accessor
    Ingredient getAddition();
}
