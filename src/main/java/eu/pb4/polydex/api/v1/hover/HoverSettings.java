package eu.pb4.polydex.api.v1.hover;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface HoverSettings {
    Identifier currentType();
    DisplayMode displayMode();

    boolean isComponentVisible(ServerPlayer player, HoverDisplayBuilder.ComponentType type);

    enum DisplayMode {
        TARGET,
        SNEAKING,
        ALWAYS
    }


    static HoverSettings get(ServerPlayer player) {
        return PolydexTarget.get(player).settings();
    }
}
