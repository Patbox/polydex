package eu.pb4.polydex.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerGameMode.class)
public interface SPIMAccessor {
    @Accessor
    boolean isIsDestroyingBlock();

    @Accessor
    boolean getHasDelayedDestroy();

    @Accessor
    int getDestroyProgressStart();

    @Accessor
    int getDelayedTickStart();

    @Accessor
    BlockPos getDestroyPos();

    @Accessor
    BlockPos getDelayedDestroyPos();

    @Accessor
    int getGameTicks();

    @Accessor
    int getLastSentState();
}
