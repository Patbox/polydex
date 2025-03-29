package eu.pb4.polydex.mixin;

import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(Ingredient.class)
public interface IngredientAccessor {
    @Accessor
    @Coerce
    Ingredient.Entry[] getEntries();
}
