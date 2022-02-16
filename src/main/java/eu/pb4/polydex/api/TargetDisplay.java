package eu.pb4.polydex.api;

import eu.pb4.polydex.impl.PlayerInterface;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import java.util.function.Function;

public interface TargetDisplay {
    void showDisplay();
    void hideDisplay();
    void onBreakingStateUpdate();
    void onTargetUpdate();

    boolean isHidden();
    boolean isSmall();
    void remove();

    static TargetDisplay get(ServerPlayerEntity player) {
        return ((PlayerInterface) player.networkHandler).polydex_getDisplay();
    }

    static void set(ServerPlayerEntity player, Identifier identifier) throws InvalidIdentifierException {
        if (PolydexImpl.DISPLAYS.containsKey(identifier)) {
            ((PlayerInterface) player.networkHandler).polydex_setDisplay(identifier, PolydexImpl.DISPLAYS.get(identifier));
        } else {
            throw new InvalidIdentifierException("TargetDisplay " + identifier + " doesn't exist!");
        }
    }

    static void register(Identifier identifier, Function<PolydexTarget, TargetDisplay> constructor) {
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
