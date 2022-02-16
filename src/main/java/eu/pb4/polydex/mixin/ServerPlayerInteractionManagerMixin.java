package eu.pb4.polydex.mixin;

import eu.pb4.polydex.impl.PlayerInterface;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;continueMining(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;I)F"))
    private void polydex_updateMining(CallbackInfo ci) {
        ((PlayerInterface) this.player.networkHandler).polydex_getTarget().onBreakingChange();
        ((PlayerInterface) this.player.networkHandler).polydex_getDisplay().onBreakingStateUpdate();
    }
}
