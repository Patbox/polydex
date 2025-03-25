package eu.pb4.polydex.mixin;

import net.minecraft.item.Item;
import net.minecraft.recipe.TransmuteRecipe;
import net.minecraft.recipe.TransmuteRecipeResult;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TransmuteRecipe.class)
public interface TransmuteRecipeAccessor {
    @Accessor
    TransmuteRecipeResult getResult();
}
