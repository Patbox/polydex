package eu.pb4.polydex.mixin;

import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerInteractionManager.class)
public interface SPIMAccessor {
    @Accessor
    boolean isMining();

    @Accessor
    boolean getFailedToMine();

    @Accessor
    int getStartMiningTime();

    @Accessor
    int getFailedStartMiningTime();

    @Accessor
    BlockPos getMiningPos();

    @Accessor
    BlockPos getFailedMiningPos();

    @Accessor
    int getTickCounter();

    @Accessor
    int getBlockBreakingProgress();
}
