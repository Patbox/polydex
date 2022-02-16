package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.PolydexTarget;
import eu.pb4.polydex.api.TargetDisplay;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public interface PlayerInterface {
    PolydexTargetImpl polydex_getTarget();
    TargetDisplay polydex_getDisplay();
    void polydex_setDisplay(Identifier identifier, Function<PolydexTarget, TargetDisplay> displayCreator);
}
