package eu.pb4.polydex.api.v1.hover;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface HoverSettings {
    Identifier currentType();
    DisplayMode displayMode();

    boolean isComponentVisible(ServerPlayerEntity player, HoverDisplayBuilder.ComponentType type);

    enum DisplayMode {
        TARGET,
        SNEAKING,
        ALWAYS
    }


    static HoverSettings get(ServerPlayerEntity player) {
        return PolydexTarget.get(player).settings();
    }
}
