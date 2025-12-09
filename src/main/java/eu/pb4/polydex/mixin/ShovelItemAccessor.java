package eu.pb4.polydex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ShovelItem.class)
public interface ShovelItemAccessor {
    @Accessor
    static Map<Block, BlockState> getFLATTENABLES() {
        throw new UnsupportedOperationException();
    }
}
