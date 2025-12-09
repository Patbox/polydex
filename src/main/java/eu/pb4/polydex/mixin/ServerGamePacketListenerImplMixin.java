package eu.pb4.polydex.mixin;

import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.impl.*;
import eu.pb4.polydex.impl.book.ui.MainIndexState;
import eu.pb4.polydex.impl.display.BossbarTargetDisplay;
import eu.pb4.polydex.impl.display.NoopTargetDisplay;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import eu.pb4.predicate.api.PredicateContext;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static eu.pb4.polydex.impl.PolydexImpl.id;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl implements PlayerInterface {
    @Shadow public ServerPlayer player;
    @Unique
    private HoverDisplay polydex_display;
    @Unique
    private PolydexTargetImpl polydex_target;
    @Unique
    private ServerPlayer polydex_oldPlayer;
    @Unique
    private int polydex_tick = 0;
    @Unique
    private final List<Identifier> polydex$lastViewed = new ArrayList<>();
    @Unique
    private boolean polydex_globalEnabled = true;
    @Unique
    private final MainIndexState mainIndexState = new MainIndexState();

    public ServerGamePacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie clientData) {
        super(server, connection, clientData);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void polydex_create(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        //noinspection ConstantConditions
        if (((Object) this).getClass() == ServerGamePacketListenerImpl.class) {
            this.polydex_target = new PolydexTargetImpl((ServerGamePacketListenerImpl) (Object) this);

            if (PolydexImpl.config.displayEnabled) {
                var creator = PolydexImpl.DISPLAYS.get(this.polydex_target.settings().currentType());
                if (creator != null) {
                    this.polydex_display = creator.apply(this.polydex_target);
                } else {
                    this.polydex_display = new BossbarTargetDisplay(this.polydex_target);
                }
            } else {
                this.polydex_display = NoopTargetDisplay.INSTANCE;
            }
        } else {
            this.polydex_display = NoopTargetDisplay.INSTANCE;
        }

        var list = PlayerDataApi.getGlobalDataFor(player, id("last_viewed"), ListTag.TYPE);

        if (list != null) {
            for (var x : list) {
                var y = Identifier.tryParse(x.asString().get());
                this.polydex$lastViewed.add(y);
            }
        }
    }

    @Inject(method = "removePlayerFromWorld", at = @At("HEAD"))
    private void saveList(CallbackInfo ci) {
        try {
            var list = new ListTag();

            for (var x : this.polydex$lastViewed) {
                list.add(StringTag.valueOf(x.toString()));
            }

            PlayerDataApi.setGlobalDataFor(player, id("last_viewed"), list);
        } catch (Throwable e) {
            // ignored
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void polydex_tick(CallbackInfo ci) {
        if (this.polydex_display == NoopTargetDisplay.INSTANCE || !PolydexImpl.config.displayEnabled) {
            return;
        }

        if (this.player.tickCount % 32 == 0) {
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
    public List<Identifier> polydex_lastViewed() {
        return this.polydex$lastViewed;
    }

    @Override
    public void polydex_setDisplay(Identifier identifier, Function<PolydexTarget, HoverDisplay> displayCreator) {
        this.polydex_display.remove();
        this.polydex_display = displayCreator.apply(this.polydex_target);
        this.polydex_target.settings().setDisplay(identifier);
    }

    @Override
    public MainIndexState polydex_mainIndexState() {
        return this.mainIndexState;
    }
}
