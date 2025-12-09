package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.impl.PlayerHoverSettings;
import eu.pb4.polydex.impl.PlayerInterface;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.mixin.SPIMAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PolydexTargetImpl implements PolydexTarget {
    private final ServerGamePacketListenerImpl handler;
    @Nullable
    private HitResult hitResult = null;
    @Nullable
    private BlockEntity cachedBlockEntity = null;
    private BlockEntity cachedMiningBlockEntity = null;

    private BlockState cachedBlockState = Blocks.AIR.defaultBlockState();
    private BlockState cachedMiningBlockState = Blocks.AIR.defaultBlockState();

    private float currentBreakingProgress = 0f;
    private int startingTime = -1;
    private BlockPos miningPos;
    private Entity entity;
    private final DisplayImpl displayBuilder;
    private final PlayerHoverSettings settings;

    public PolydexTargetImpl(ServerGamePacketListenerImpl handler) {
        this.handler = handler;
        this.settings = new PlayerHoverSettings(handler);
        this.displayBuilder = new DisplayImpl(this);
    }

    public static PolydexTargetImpl get(ServerPlayer player) {
        return ((PlayerInterface) player.connection).polydex_getTarget();
    }

    public void updateRaycast() {
        var player = this.player();
        double maxDistance = this.player().blockInteractionRange();

        this.hitResult = this.handler.player.pick(maxDistance, 0, false);

        if (PolydexImpl.config.displayEntity) {
            Vec3 min = player.getEyePosition(0);
            var sqrdDist = maxDistance * maxDistance;

            if (this.hitResult != null) {
                sqrdDist = this.hitResult.getLocation().distanceToSqr(player.getEyePosition());
            }

            Vec3 vec3d2 = player.getViewVector(1.0F);
            Vec3 max = min.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);

            AABB box = player.getBoundingBox().expandTowards(vec3d2.scale(maxDistance)).inflate(1.0D, 1.0D, 1.0D);
            var hitResult = ProjectileUtil.getEntityHitResult(this.handler.player, min, max, box, this::isTargettable, sqrdDist);

            if (hitResult != null) {
                this.hitResult = hitResult;
                this.entity = hitResult.getEntity();
            }
        }

        if (this.hitResult.getType() == HitResult.Type.BLOCK) {
            var result = (BlockHitResult) this.hitResult;
            this.cachedBlockState = this.handler.player.level().getBlockState(result.getBlockPos());
            if (this.cachedBlockState.hasBlockEntity()) {
                this.cachedBlockEntity = this.handler.player.level().getBlockEntity(result.getBlockPos());
            } else {
                this.cachedBlockEntity = null;
            }
            this.entity = null;
        } else {
            this.cachedBlockState = Blocks.AIR.defaultBlockState();
            this.cachedBlockEntity = null;
            if (this.hitResult.getType() == HitResult.Type.MISS) {
                this.entity = null;
            }
        }
    }

    private boolean isTargettable(Entity entity) {
        return !entity.isSpectator() && entity.isPickable() && !entity.isInvisibleTo(this.player());
    }

    public void onBreakingChange() {
        var inter = (SPIMAccessor) this.player().gameMode;
        if (inter.getHasDelayedDestroy()) {
            var state = this.player().level().getBlockState(inter.getDelayedDestroyPos());

            if (!inter.getDestroyPos().equals(this.miningPos) || this.startingTime != inter.getDelayedTickStart()) {
                this.currentBreakingProgress = 0f;
                this.startingTime = inter.getDelayedTickStart();
                this.miningPos = inter.getDestroyPos();
                this.cachedMiningBlockEntity = this.player().level().getBlockEntity(this.miningPos);
                this.cachedMiningBlockState = state;
            }

            this.currentBreakingProgress = Math.min(
                    this.currentBreakingProgress + state.getDestroyProgress(this.player(), this.player().level(), inter.getDelayedDestroyPos()),
                    1
            );
        } else {
            var state = this.player().level().getBlockState(inter.getDestroyPos());
            if (!inter.getDestroyPos().equals(this.miningPos) || this.startingTime != inter.getDestroyProgressStart()) {
                this.currentBreakingProgress = 0f;
                this.startingTime = inter.getDestroyProgressStart();
                this.miningPos = inter.getDestroyPos();
                this.cachedMiningBlockEntity = this.player().level().getBlockEntity(this.miningPos);
                this.cachedMiningBlockState = state;
            }

            if (inter.isIsDestroyingBlock()) {
                this.currentBreakingProgress = Math.min(
                        this.currentBreakingProgress + state.getDestroyProgress(this.player(), this.player().level(), inter.getDestroyPos()),
                        1
                );
            }
        }
    }

    @Override
    public ServerPlayer player() {
        return this.handler.player;
    }

    @Override
    public HitResult hitResult() {
        return this.hitResult;
    }

    @Override
    public BlockState blockState() {
        return this.getIntMen().isIsDestroyingBlock() ? this.cachedMiningBlockState : this.cachedBlockState;
    }

    @Override
    @Nullable
    public BlockEntity blockEntity() {
        return this.getIntMen().isIsDestroyingBlock() ? this.cachedMiningBlockEntity : this.cachedBlockEntity;
    }

    @Override
    public @Nullable Entity entity() {
        return this.entity;
    }

    @Override
    public BlockPos pos() {
        return this.getIntMen().isIsDestroyingBlock() ? this.miningPos : this.hitResult instanceof BlockHitResult blockHitResult ? blockHitResult.getBlockPos() : this.entity != null ? this.entity.blockPosition() : BlockPos.ZERO;
    }

    @Override
    public float breakingProgress() {
        return this.getIntMen().isIsDestroyingBlock() ? this.currentBreakingProgress : 0f;
    }

    @Override
    public boolean isMining() {
        return this.getIntMen().isIsDestroyingBlock() || this.getIntMen().getHasDelayedDestroy();
    }

    @Override
    public boolean hasTarget() {
        return !this.blockState().isAir() || this.entity != null;
    }

    @Override
    public PlayerHoverSettings settings() {
        return this.settings;
    }

    public DisplayImpl getDisplayBuilder() {
        return this.displayBuilder;
    }


    private SPIMAccessor getIntMen() {
        return (SPIMAccessor) this.handler.player.gameMode;
    }

    public HoverDisplay getDisplay() {
        return ((PlayerInterface) this.handler).polydex_getDisplay();
    }
}
