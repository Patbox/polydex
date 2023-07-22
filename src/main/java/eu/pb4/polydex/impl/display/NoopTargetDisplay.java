package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;

public class NoopTargetDisplay implements HoverDisplay {
    public static final HoverDisplay INSTANCE = new NoopTargetDisplay();

    @Override
    public void showDisplay() {

    }

    @Override
    public void hideDisplay() {

    }

    @Override
    public void onBreakingStateUpdate() {

    }

    @Override
    public void onTargetUpdate() {

    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isSmall() {
        return true;
    }

    @Override
    public void remove() {

    }

    @Override
    public Type getType() {
        return Type.NONE;
    }

    public static HoverDisplay create(PolydexTarget target) {
        return INSTANCE;
    }
}
