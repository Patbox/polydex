package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DisplayImpl implements HoverDisplayBuilder {
    private final PolydexTargetImpl target;
    private final Map<ComponentType, Text> components = new HashMap<>();

    public DisplayImpl(PolydexTargetImpl target) {
        this.target = target;
    }

    @Override
    public boolean isSmall() {
        return this.target.getDisplay().isSmall();
    }

    @Override
    public HoverDisplay.Type getDisplayType() {
        return this.target.getDisplay().getType();
    }

    @Override
    public PolydexTarget getTarget() {
        return this.target;
    }

    @Override
    public void setComponent(ComponentType type, Text text) {
        if (this.target.settings().isComponentVisible(this.target.player(), type)) {
            this.components.put(type, text);
        }
    }

    @Override
    public Text getComponent(ComponentType type) {
        return this.components.get(type);
    }

    @Override
    public boolean removeComponent(ComponentType type) {
        return this.removeAndGetComponent(type) != null;
    }

    @Override
    public @Nullable Text removeAndGetComponent(ComponentType type) {
        return this.components.remove(type);
    }

    @Override
    public Collection<ComponentType> getComponentTypes() {
        return this.components.keySet();
    }

    @Override
    public List<Text> getOutput() {
        var list = new ArrayList<>(this.components.entrySet());
        list.sort(Comparator.comparing((t) -> t.getKey().index()));
        var out = new ArrayList<Text>();

        for (var entry : list) {
            out.add(entry.getValue());
        }

        return out;
    }

    public void clear() {
        this.components.clear();
    }
}
