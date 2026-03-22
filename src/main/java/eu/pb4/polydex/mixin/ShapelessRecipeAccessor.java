package eu.pb4.polydex.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShapelessRecipe.class)
public interface ShapelessRecipeAccessor {
    @Accessor
    ItemStackTemplate getResult();
}
