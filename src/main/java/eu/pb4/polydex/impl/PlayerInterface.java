package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.hover.PolydexTarget;
import eu.pb4.polydex.api.hover.HoverDisplay;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public interface PlayerInterface {
    PolydexTargetImpl polydex_getTarget();
    HoverDisplay polydex_getDisplay();
    void polydex_setDisplay(Identifier identifier, Function<PolydexTarget, HoverDisplay> displayCreator);
}
