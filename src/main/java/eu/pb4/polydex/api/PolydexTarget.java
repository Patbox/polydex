package eu.pb4.polydex.api;

import eu.pb4.polydex.impl.PlayerInterface;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface PolydexTarget {
    ServerPlayerEntity getPlayer();
    @Nullable
    HitResult getHitResult();
    BlockState getBlockState();
    @Nullable
    BlockEntity getBlockEntity();

    @Nullable
    Entity getEntity();

    BlockPos getTargetPos();

    float getBreakingProgress();
    boolean isMining();

    boolean hasTarget();

    static PolydexTarget get(ServerPlayerEntity player) {
        return ((PlayerInterface) player.networkHandler).polydex_getTarget();
    }
}
