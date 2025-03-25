package eu.pb4.polydex.mixin;

import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.recipe.SmithingTrimRecipe;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingTrimRecipe.class)
public interface SmithingTrimRecipeAccessor {
    @Accessor
    RegistryEntry<ArmorTrimPattern> getPattern();
}
