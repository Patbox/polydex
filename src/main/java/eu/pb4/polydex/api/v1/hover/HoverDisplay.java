package eu.pb4.polydex.api.v1.hover;

import eu.pb4.polydex.impl.PlayerInterface;
import eu.pb4.polydex.impl.PolydexImpl;
import java.util.function.Function;
import net.minecraft.IdentifierException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public interface HoverDisplay {
    void showDisplay();
    void hideDisplay();
    void onBreakingStateUpdate();
    void onTargetUpdate();

    boolean isHidden();
    boolean isSmall();
    void remove();

    static HoverDisplay get(ServerPlayer player) {
        return ((PlayerInterface) player.connection).polydex_getDisplay();
    }

    static void set(ServerPlayer player, Identifier identifier) throws IdentifierException {
        if (PolydexImpl.DISPLAYS.containsKey(identifier)) {
            ((PlayerInterface) player.connection).polydex_setDisplay(identifier, PolydexImpl.DISPLAYS.get(identifier));
        } else {
            throw new IdentifierException("HoverDisplay " + identifier + " doesn't exist!");
        }
    }

    static void register(Identifier identifier, Function<PolydexTarget, HoverDisplay> constructor) {
        PolydexImpl.DISPLAYS.put(identifier, constructor);
    }

    Type getType();

    enum Type {
        NONE,
        PLACEHOLDER,
        MULTI_LINE,
        SINGLE_LINE,
    }
}
