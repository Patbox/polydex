package eu.pb4.polydex.mixin;

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStackSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackSet.class)
public interface ItemStackSetAccessor {
    @Accessor
    static Hash.Strategy<? super ItemStack> getHASH_STRATEGY() {
        throw new UnsupportedOperationException();
    }
}
