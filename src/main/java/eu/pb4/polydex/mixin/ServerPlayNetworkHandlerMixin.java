package eu.pb4.polydex.mixin;

import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.impl.*;
import eu.pb4.polydex.impl.display.BossbarTargetDisplay;
import eu.pb4.polydex.impl.display.NoopTargetDisplay;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import eu.pb4.polymer.core.impl.PolymerImpl;
import eu.pb4.predicate.api.PredicateContext;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
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
    private HoverDisplay polydex_display;

    @Shadow public abstract void sendPacket(Packet<?> packet);

    @Unique
    private PolydexTargetImpl polydex_target;
    @Unique
    private ServerPlayerEntity polydex_oldPlayer;
    @Unique
    private int polydex_tick = 0;
    @Unique
    private boolean polydex_globalEnabled = true;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void polydex_create(MinecraftServer server, ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        //noinspection ConstantConditions
        if (((Object) this).getClass() == ServerPlayNetworkHandler.class) {
            this.polydex_target = new PolydexTargetImpl((ServerPlayNetworkHandler) (Object) this);

            var creator = PolydexImpl.DISPLAYS.get(this.polydex_target.settings().currentType());
            if (creator != null) {
                this.polydex_display = creator.apply(this.polydex_target);
            } else {
                this.polydex_display = new BossbarTargetDisplay(this.polydex_target);
            }
        } else {
            this.polydex_display = NoopTargetDisplay.INSTANCE;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void polydex_tick(CallbackInfo ci) {
        if (this.polydex_display == NoopTargetDisplay.INSTANCE) {
            return;
        }

        if (this.player.age % 32 == 0) {
            var value = PolydexImpl.config.displayPredicate.test(PredicateContext.of(this.player)).success();

            if (this.polydex_globalEnabled != value) {
                this.polydex_globalEnabled = value;
                if (!value && !this.polydex_display.isHidden()) {
                    this.polydex_display.hideDisplay();
                }
                return;
            }
        }

        if (!this.polydex_globalEnabled) {
            return;
        }

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
    public HoverDisplay polydex_getDisplay() {
        return this.polydex_display;
    }

    @Override
    public void polydex_setDisplay(Identifier identifier, Function<PolydexTarget, HoverDisplay> displayCreator) {
        this.polydex_display.remove();
        this.polydex_display = displayCreator.apply(this.polydex_target);
        this.polydex_target.settings().setDisplay(identifier);

    }
}
