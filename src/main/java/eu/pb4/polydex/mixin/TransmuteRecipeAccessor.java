package eu.pb4.polydex.mixin;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TransmuteRecipe.class)
public interface TransmuteRecipeAccessor {
    @Accessor
    TransmuteResult getResult();

    @Accessor
    Ingredient getInput();
}
