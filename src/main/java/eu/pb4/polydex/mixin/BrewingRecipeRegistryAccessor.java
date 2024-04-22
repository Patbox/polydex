package eu.pb4.polydex.mixin;

import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BrewingRecipeRegistry.class)
public interface BrewingRecipeRegistryAccessor {
    @Accessor
    List<BrewingRecipeRegistry.Recipe<Potion>> getPotionRecipes();

    @Accessor
    List<BrewingRecipeRegistry.Recipe<Item>> getItemRecipes();
}
