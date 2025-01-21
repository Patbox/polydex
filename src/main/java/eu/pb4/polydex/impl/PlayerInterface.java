package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.impl.book.ui.MainIndexState;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Function;

public interface PlayerInterface {
    PolydexTargetImpl polydex_getTarget();
    HoverDisplay polydex_getDisplay();
    List<Identifier> polydex_lastViewed();
    MainIndexState polydex_mainIndexState();
    void polydex_setDisplay(Identifier identifier, Function<PolydexTarget, HoverDisplay> displayCreator);


    static void addViewed(ServerPlayerEntity player, Identifier identifier) {
        var list = ((PlayerInterface) player.networkHandler).polydex_lastViewed();
        if (list.contains(identifier)) {
            list.remove(identifier);
            list.addFirst(identifier);
        } else {
            while (list.size() > 127) {
                list.removeLast();
            }
            list.addFirst(identifier);
        }
    }
}
