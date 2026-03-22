package eu.pb4.polydex.mixin;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TransmuteRecipe.class)
public interface TransmuteRecipeAccessor {
    @Accessor
    ItemStackTemplate getResult();
}
