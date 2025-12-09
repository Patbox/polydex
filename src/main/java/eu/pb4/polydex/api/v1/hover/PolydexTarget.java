package eu.pb4.polydex.api.v1.hover;

import eu.pb4.polydex.impl.PlayerInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface PolydexTarget {
    ServerPlayer player();
    @Nullable
    HitResult hitResult();
    BlockState blockState();
    @Nullable
    BlockEntity blockEntity();

    @Nullable
    Entity entity();
    BlockPos pos();

    float breakingProgress();
    boolean isMining();

    boolean hasTarget();

    HoverSettings settings();

    static PolydexTarget get(ServerPlayer player) {
        return ((PlayerInterface) player.connection).polydex_getTarget();
    }
}
