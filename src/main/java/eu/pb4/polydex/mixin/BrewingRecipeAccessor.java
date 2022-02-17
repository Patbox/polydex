package eu.pb4.polydex.mixin;

import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrewingRecipeRegistry.Recipe.class)
public interface BrewingRecipeAccessor<T> {
    @Accessor
    T getInput();

    @Accessor
    Ingredient getIngredient();

    @Accessor
    T getOutput();
}
