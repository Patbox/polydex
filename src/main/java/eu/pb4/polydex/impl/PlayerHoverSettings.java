package eu.pb4.polydex.impl;

import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.HoverSettings;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static eu.pb4.polydex.impl.PolydexImpl.id;

public class PlayerHoverSettings implements HoverSettings {
    private final ServerPlayNetworkHandler handler;
    private final Map<Identifier, HoverDisplayBuilder.ComponentType.Visibility> componentMap = new HashMap<>();
    @Nullable
    public Identifier currentDisplay;
    private DisplayMode displayMode;

    public PlayerHoverSettings(ServerPlayNetworkHandler handler) {
        this.handler = handler;
        var player = handler.player;

        if (PlayerDataApi.getGlobalDataFor(player, id("display_type")) instanceof NbtString nbtString) {
            this.currentDisplay = Identifier.tryParse(nbtString.asString());
        }

        if (PlayerDataApi.getGlobalDataFor(player, id("components")) instanceof NbtCompound compound) {
            for (var key : compound.getKeys()) {
                var id = Identifier.tryParse(key);

                try {
                    if (id != null) {
                        this.componentMap.put(id, HoverDisplayBuilder.ComponentType.Visibility.valueOf(compound.getString(key)));
                    }
                } catch (Throwable e) {

                }
            }
        }

        if (PlayerDataApi.getGlobalDataFor(player, id("display_mode")) instanceof NbtString string) {
            try {
                displayMode = DisplayMode.valueOf(string.asString());
            } catch (Throwable e) {}
        }
    }

    @Override
    public Identifier currentType() {
        return this.currentDisplay != null ? this.currentDisplay : PolydexImpl.config.defaultHoverSettings.currentType();
    }

    @Override
    public DisplayMode displayMode() {
        return this.displayMode != null ? this.displayMode : PolydexImpl.config.defaultHoverSettings.displayMode();
    }

    @Override
    public boolean isComponentVisible(ServerPlayerEntity player, HoverDisplayBuilder.ComponentType type) {
        var value = componentMap.getOrDefault(type.identifier(), HoverDisplayBuilder.ComponentType.Visibility.DEFAULT);

        return switch (value) {
            case ALWAYS -> true;
            case NEVER -> false;
            case SNEAKING -> player.isSneaking();
            case DEFAULT -> PolydexImpl.config.defaultHoverSettings.isComponentVisible(player, type);
        };
    }

    public void setComponentVisible(Identifier identifier, HoverDisplayBuilder.ComponentType.Visibility value) {
        componentMap.put(identifier, value);

        NbtCompound nbtCompound;
        if (PlayerDataApi.getGlobalDataFor(this.handler.player, id("components")) instanceof NbtCompound compound) {
            nbtCompound = compound;
        } else {
            nbtCompound = new NbtCompound();
            PlayerDataApi.setGlobalDataFor(this.handler.player, id("components"), nbtCompound);
        }
        nbtCompound.putString(identifier.toString(), value.name());
    }

    public void setDisplay(@Nullable Identifier identifier) {
        this.currentDisplay = identifier;
        PlayerDataApi.setGlobalDataFor(this.handler.player, id("display_type"), identifier != null ? NbtString.of(identifier.toString()) : null);
    }

    public void setDisplayMode(@Nullable DisplayMode value) {
        this.displayMode = value;
        PlayerDataApi.setGlobalDataFor(this.handler.player, id("display_mode"), value != null ? NbtString.of(value.toString()) : null);
        try {
            HoverDisplay.set(this.handler.player, this.currentType());
        } catch (Throwable e) {
            this.currentDisplay = null;
        }
    }
}
