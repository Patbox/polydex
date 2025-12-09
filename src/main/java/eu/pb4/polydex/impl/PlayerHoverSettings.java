package eu.pb4.polydex.impl;

import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.HoverSettings;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import static eu.pb4.polydex.impl.PolydexImpl.id;

public class PlayerHoverSettings implements HoverSettings {
    private final ServerGamePacketListenerImpl handler;
    private final Map<Identifier, HoverDisplayBuilder.ComponentType.Visibility> componentMap = new HashMap<>();
    @Nullable
    public Identifier currentDisplay;
    private DisplayMode displayMode;

    public PlayerHoverSettings(ServerGamePacketListenerImpl handler) {
        this.handler = handler;
        var player = handler.player;

        if (PlayerDataApi.getGlobalDataFor(player, id("display_type")) instanceof StringTag nbtString) {
            this.currentDisplay = Identifier.tryParse(nbtString.value());
        }

        if (PlayerDataApi.getGlobalDataFor(player, id("components")) instanceof CompoundTag compound) {
            for (var key : compound.keySet()) {
                var id = Identifier.tryParse(key);

                try {
                    if (id != null) {
                        this.componentMap.put(id, HoverDisplayBuilder.ComponentType.Visibility.valueOf(compound.getStringOr(key, "")));
                    }
                } catch (Throwable e) {

                }
            }
        }

        if (PlayerDataApi.getGlobalDataFor(player, id("display_mode")) instanceof StringTag string) {
            try {
                displayMode = DisplayMode.valueOf(string.value());
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
    public boolean isComponentVisible(ServerPlayer player, HoverDisplayBuilder.ComponentType type) {
        var value = componentMap.getOrDefault(type.identifier(), HoverDisplayBuilder.ComponentType.Visibility.DEFAULT);

        return switch (value) {
            case ALWAYS -> true;
            case NEVER -> false;
            case SNEAKING -> player.isShiftKeyDown();
            case DEFAULT -> PolydexImpl.config.defaultHoverSettings.isComponentVisible(player, type);
        };
    }

    public void setComponentVisible(Identifier identifier, HoverDisplayBuilder.ComponentType.Visibility value) {
        componentMap.put(identifier, value);

        CompoundTag nbtCompound;
        if (PlayerDataApi.getGlobalDataFor(this.handler.player, id("components")) instanceof CompoundTag compound) {
            nbtCompound = compound;
        } else {
            nbtCompound = new CompoundTag();
            PlayerDataApi.setGlobalDataFor(this.handler.player, id("components"), nbtCompound);
        }
        nbtCompound.putString(identifier.toString(), value.name());
    }

    public void setDisplay(@Nullable Identifier identifier) {
        this.currentDisplay = identifier;
        PlayerDataApi.setGlobalDataFor(this.handler.player, id("display_type"), identifier != null ? StringTag.valueOf(identifier.toString()) : null);
    }

    public void setDisplayMode(@Nullable DisplayMode value) {
        this.displayMode = value;
        PlayerDataApi.setGlobalDataFor(this.handler.player, id("display_mode"), value != null ? StringTag.valueOf(value.toString()) : null);
        try {
            HoverDisplay.set(this.handler.player, this.currentType());
        } catch (Throwable e) {
            this.currentDisplay = null;
        }
    }
}
