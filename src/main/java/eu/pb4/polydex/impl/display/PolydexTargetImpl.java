package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.hover.PolydexTarget;
import eu.pb4.polydex.api.hover.HoverDisplay;
import eu.pb4.polydex.impl.PlayerInterface;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.mixin.SPIMAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public final class PolydexTargetImpl implements PolydexTarget {
    private final ServerPlayNetworkHandler handler;
    @Nullable
    private HitResult hitResult = null;
    @Nullable
    private BlockEntity cachedBlockEntity = null;
    private BlockEntity cachedMiningBlockEntity = null;

    private BlockState cachedBlockState = Blocks.AIR.getDefaultState();
    private BlockState cachedMiningBlockState = Blocks.AIR.getDefaultState();

    private float currentBreakingProgress = 0f;
    private int startingTime = -1;
    private BlockPos miningPos;
    private Entity entity;
    private DisplayImpl displayBuilder;

    public PolydexTargetImpl(ServerPlayNetworkHandler handler) {
        this.handler = handler;
        this.displayBuilder = new DisplayImpl(this);
    }

    public void updateRaycast() {
        var player = this.getPlayer();
        double maxDistance = 8.02;

        this.hitResult = this.handler.player.raycast(maxDistance, 0, false);

        if (PolydexImpl.config.displayEntity) {
            Vec3d min = player.getCameraPosVec(0);
            var sqrdDist = maxDistance * maxDistance;

            if (this.hitResult != null) {
                sqrdDist = this.hitResult.getPos().squaredDistanceTo(player.getEyePos());
            }

            Vec3d vec3d2 = player.getRotationVec(1.0F);
            Vec3d max = min.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);

            Box box = player.getBoundingBox().stretch(vec3d2.multiply(maxDistance)).expand(1.0D, 1.0D, 1.0D);
            var hitResult = ProjectileUtil.raycast(this.handler.player, min, max, box, (entityx) -> !entityx.isSpectator() && entityx.canHit(), sqrdDist);

            if (hitResult != null) {
                this.hitResult = hitResult;
                this.entity = hitResult.getEntity();
            }
        }

        if (this.hitResult.getType() == HitResult.Type.BLOCK) {
            var result = (BlockHitResult) this.hitResult;
            this.cachedBlockState = this.handler.player.getWorld().getBlockState(result.getBlockPos());
            if (this.cachedBlockState.hasBlockEntity()) {
                this.cachedBlockEntity = this.handler.player.getWorld().getBlockEntity(result.getBlockPos());
            } else {
                this.cachedBlockEntity = null;
            }
            this.entity = null;
        } else {
            this.cachedBlockState = Blocks.AIR.getDefaultState();
            this.cachedBlockEntity = null;
            if (this.hitResult.getType() == HitResult.Type.MISS) {
                this.entity = null;
            }
        }
    }

    public void onBreakingChange() {
        var inter = (SPIMAccessor) this.getPlayer().interactionManager;
        if (inter.getFailedToMine()) {
            var state = this.getPlayer().getWorld().getBlockState(inter.getFailedMiningPos());

            if (!inter.getMiningPos().equals(this.miningPos) || this.startingTime != inter.getFailedStartMiningTime()) {
                this.currentBreakingProgress = 0f;
                this.startingTime = inter.getFailedStartMiningTime();
                this.miningPos = inter.getFailedMiningPos();
                this.cachedMiningBlockEntity = this.getPlayer().getWorld().getBlockEntity(this.miningPos);
                this.cachedMiningBlockState = state;
            }

            this.currentBreakingProgress = Math.min(
                    this.currentBreakingProgress + state.calcBlockBreakingDelta(this.getPlayer(), this.getPlayer().getWorld(), inter.getFailedMiningPos()),
                    1
            );
        } else {
            var state = this.getPlayer().getWorld().getBlockState(inter.getMiningPos());
            if (!inter.getMiningPos().equals(this.miningPos) || this.startingTime != inter.getStartMiningTime()) {
                this.currentBreakingProgress = 0f;
                this.startingTime = inter.getStartMiningTime();
                this.miningPos = inter.getMiningPos();
                this.cachedMiningBlockEntity = this.getPlayer().getWorld().getBlockEntity(this.miningPos);
                this.cachedMiningBlockState = state;
            }

            if (inter.isMining()) {
                this.currentBreakingProgress = Math.min(
                        this.currentBreakingProgress + state.calcBlockBreakingDelta(this.getPlayer(), this.getPlayer().getWorld(), inter.getMiningPos()),
                        1
                );
            }
        }
    }

    @Override
    public ServerPlayerEntity getPlayer() {
        return this.handler.player;
    }

    @Override
    public HitResult getHitResult() {
        return this.hitResult;
    }

    @Override
    public BlockState getBlockState() {
        return this.getIntMen().isMining() ? this.cachedMiningBlockState : this.cachedBlockState;
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity() {
        return this.getIntMen().isMining() ? this.cachedMiningBlockEntity : this.cachedBlockEntity;
    }

    @Override
    public @Nullable Entity getEntity() {
        return this.entity;
    }

    @Override
    public BlockPos getTargetPos() {
        return this.getIntMen().isMining() ? this.miningPos : this.hitResult instanceof BlockHitResult blockHitResult ? blockHitResult.getBlockPos() : this.entity != null ? this.entity.getBlockPos() : BlockPos.ORIGIN;
    }

    @Override
    public float getBreakingProgress() {
        return this.getIntMen().isMining() ? this.currentBreakingProgress : 0f;
    }

    @Override
    public boolean isMining() {
        return this.getIntMen().isMining() || this.getIntMen().getFailedToMine();
    }

    @Override
    public boolean hasTarget() {
        return !this.getBlockState().isAir() || this.entity != null;
    }

    public DisplayImpl getDisplayBuilder() {
        return this.displayBuilder;
    }


    private SPIMAccessor getIntMen() {
        return (SPIMAccessor) this.handler.player.interactionManager;
    }

    public HoverDisplay getDisplay() {
        return ((PlayerInterface) this.handler).polydex_getDisplay();
    }
}
