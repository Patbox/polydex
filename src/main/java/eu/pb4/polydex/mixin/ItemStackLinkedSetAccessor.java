package eu.pb4.polydex.mixin;

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackLinkedSet.class)
public interface ItemStackLinkedSetAccessor {
    @Accessor
    static Hash.Strategy<? super ItemStack> getTYPE_AND_TAG() {
        throw new UnsupportedOperationException();
    }
}
