package eu.pb4.polydex.mixin;

import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.polydex.api.PolydexTarget;
import eu.pb4.polydex.api.TargetDisplay;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.display.BossbarTargetDisplay;
import eu.pb4.polydex.impl.PlayerInterface;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

import static eu.pb4.polydex.impl.PolydexImpl.id;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements PlayerInterface {
    @Shadow public ServerPlayerEntity player;
    private TargetDisplay polydex_display;

    @Shadow public abstract void sendPacket(Packet<?> packet);

    @Unique
    private PolydexTargetImpl polydex_target;
    @Unique
    private ServerPlayerEntity polydex_oldPlayer;
    @Unique
    private int polydex_tick = 0;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void polydex_create(MinecraftServer server, ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        this.polydex_target = new PolydexTargetImpl((ServerPlayNetworkHandler) (Object) this);

        var string = PlayerDataApi.getGlobalDataFor(player, id("display_type"));

        Identifier display = null;
        if (string instanceof NbtString nbtString) {
            display = Identifier.tryParse(nbtString.asString());
        }

        if (display == null) {
            display = PolydexImpl.config.defaultDisplay;
        }

        var creator = PolydexImpl.DISPLAYS.get(display);
        if (creator != null) {
            this.polydex_display = creator.apply(this.polydex_target);
        } else {
            this.polydex_display = new BossbarTargetDisplay(this.polydex_target, false);
        }

    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void polydex_tick(CallbackInfo ci) {
        if (this.player != this.polydex_oldPlayer) {
            this.polydex_oldPlayer = this.player;
            this.polydex_update();
            return;
        }

        if (this.polydex_tick == PolydexImpl.config.displayUpdateRate) {
            this.polydex_tick = 0;
            this.polydex_update();
        }
        this.polydex_tick++;
    }

    private void polydex_update() {
        this.polydex_target.updateRaycast();

        if (this.polydex_display.isHidden() && this.polydex_target.hasTarget()) {
            this.polydex_display.showDisplay();
        } else if (!this.polydex_display.isHidden()) {
            if (!this.polydex_target.hasTarget()) {
                this.polydex_display.hideDisplay();
            } else {
                this.polydex_display.onTargetUpdate();

            }
        }
    }

    @Override
    public PolydexTargetImpl polydex_getTarget() {
        return this.polydex_target;
    }

    @Override
    public TargetDisplay polydex_getDisplay() {
        return this.polydex_display;
    }

    @Override
    public void polydex_setDisplay(Identifier identifier, Function<PolydexTarget, TargetDisplay> displayCreator) {
        this.polydex_display.remove();
        this.polydex_display = displayCreator.apply(this.polydex_target);
        PlayerDataApi.setGlobalDataFor(this.player, id("display_type"), NbtString.of(identifier.toString()));
    }
}
