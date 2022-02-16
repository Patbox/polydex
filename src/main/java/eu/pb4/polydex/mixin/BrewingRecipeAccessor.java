package eu.pb4.polydex.mixin;

import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.recipe.BrewingRecipeRegistry$Recipe")
public interface BrewingRecipeAccessor<T> {
    @Accessor
    T getInput();

    @Accessor
    Ingredient getIngredient();

    @Accessor
    T getOutput();
}
