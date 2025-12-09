package eu.pb4.polydex.mixin;

import eu.pb4.polydex.impl.PlayerInterface;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;incrementDestroyProgress(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;I)F"))
    private void polydex_updateMining(CallbackInfo ci) {
        if (this.player.connection.getClass() == ServerGamePacketListenerImpl.class) {
            ((PlayerInterface) this.player.connection).polydex_getTarget().onBreakingChange();
            ((PlayerInterface) this.player.connection).polydex_getDisplay().onBreakingStateUpdate();
        }
    }
}
