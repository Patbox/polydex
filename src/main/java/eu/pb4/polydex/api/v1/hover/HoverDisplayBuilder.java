package eu.pb4.polydex.api.v1.hover;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static eu.pb4.polydex.impl.PolydexImpl.id;
@ApiStatus.NonExtendable
public interface HoverDisplayBuilder {
    ComponentType NAME = ComponentType.of(id("name"), true);
    ComponentType MOD_SOURCE = ComponentType.of(id("mod_source"), false);
    ComponentType HEALTH = ComponentType.of(id("health"), true);
    ComponentType EFFECTS = ComponentType.of(id("effects"), true);
    ComponentType INPUT = ComponentType.of(id("input"), false);
    ComponentType FUEL = ComponentType.of(id("fuel"), false);
    ComponentType OUTPUT = ComponentType.of(id("output"), false);
    ComponentType PROGRESS = ComponentType.of(id("progress"), true);

    boolean isSmall();
    HoverDisplay.Type getDisplayType();
    PolydexTarget getTarget();
    void setComponent(ComponentType type, Text text);
    Text getComponent(ComponentType type);
    boolean removeComponent(ComponentType type);
    @Nullable
    Text removeAndGetComponent(ComponentType type);
    Collection<ComponentType> getComponentTypes();
    List<Text> getOutput();



    static void register(Consumer<HoverDisplayBuilder> consumer) {
        PolydexImpl.DISPLAY_BUILDER_CONSUMERS.add(consumer);
    }

    static HoverDisplayBuilder build(PolydexTarget target) {
        var builder = ((PolydexTargetImpl) target).getDisplayBuilder();
        builder.clear();
        if (builder.getDisplayType() != HoverDisplay.Type.NONE) {
            for (var consumer : PolydexImpl.DISPLAY_BUILDER_CONSUMERS) {
                consumer.accept(builder);
            }
        }
        return builder;
    }

    static List<Text> buildText(PolydexTarget target) {
        return build(target).getOutput();
    }

    record ComponentType(Identifier identifier, boolean alwaysDisplay, int index) {
        private static int currentIndex = 0;

        public static ComponentType of(Identifier identifier, boolean alwaysDisplay) {
            return new ComponentType(identifier, alwaysDisplay, currentIndex++);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComponentType component = (ComponentType) o;
            return Objects.equals(identifier, component.identifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier);
        }
    }
}