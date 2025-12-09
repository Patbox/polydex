package eu.pb4.polydex.api.v1.hover;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import static eu.pb4.polydex.impl.PolydexImpl.id;

@ApiStatus.NonExtendable
public interface HoverDisplayBuilder {
    ComponentType NAME = ComponentType.of(id("name"), true);
    ComponentType MOD_SOURCE = ComponentType.of(id("mod_source"), false);
    ComponentType HEALTH = ComponentType.of(id("health"), true);
    ComponentType ARMOR = ComponentType.of(id("armor"), true);
    ComponentType EFFECTS = ComponentType.of(id("effects"), true);
    ComponentType INPUT = ComponentType.of(id("input"), false);
    ComponentType FUEL = ComponentType.of(id("fuel"), false);
    ComponentType OUTPUT = ComponentType.of(id("output"), false);
    ComponentType PROGRESS = ComponentType.of(id("progress"), true);
    ComponentType RAW_ID = ComponentType.of(id("raw_id"), false);

    static void register(Consumer<HoverDisplayBuilder> consumer) {
        PolydexImpl.DISPLAY_BUILDER_CONSUMERS.add(consumer);
    }

    static void register(Block block, Consumer<HoverDisplayBuilder> consumer) {
        PolydexImpl.DISPLAY_BUILDER_CONSUMERS_BLOCK.computeIfAbsent(block, (x) -> new ArrayList<>()).add(consumer);
    }

    static void register(EntityType<?> type, Consumer<HoverDisplayBuilder> consumer) {
        PolydexImpl.DISPLAY_BUILDER_CONSUMERS_ENTITY_TYPE.computeIfAbsent(type, (x) -> new ArrayList<>()).add(consumer);
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

    static List<Component> buildText(PolydexTarget target) {
        return build(target).getOutput();
    }

    boolean isSmall();

    HoverDisplay.Type getDisplayType();

    PolydexTarget getTarget();

    void setComponent(ComponentType type, Component text);

    @Nullable
    Component getComponent(ComponentType type);

    boolean removeComponent(ComponentType type);

    @Nullable
    Component removeAndGetComponent(ComponentType type);

    Collection<ComponentType> getComponentTypes();

    List<Component> getOutput();

    record ComponentType(Identifier identifier, Visibility defaultVisibility, @Deprecated boolean alwaysDisplay,
                         int index) {
        private static final Set<ComponentType> KNOWN_COMPONENTS = new HashSet<>();
        private static final Map<Identifier, ComponentType> ID = new HashMap<>();
        private static int currentIndex = 0;

        @Deprecated
        public ComponentType(Identifier identifier, @Deprecated boolean alwaysDisplay, int index) {
            this(identifier, alwaysDisplay ? Visibility.ALWAYS : Visibility.NEVER, alwaysDisplay, index);
        }


        public ComponentType {
            if (ID.containsKey(identifier)) {
                throw new RuntimeException("Duplicate ComponentType '" + identifier + "'");
            }

            KNOWN_COMPONENTS.add(this);
            ID.put(identifier, this);
        }

        public static ComponentType of(Identifier identifier, boolean alwaysDisplay) {
            return new ComponentType(identifier, alwaysDisplay ? Visibility.ALWAYS : Visibility.NEVER, alwaysDisplay, currentIndex++);
        }

        public static ComponentType of(Identifier identifier, Visibility displayMode) {
            return new ComponentType(identifier, displayMode, displayMode == Visibility.ALWAYS, currentIndex++);
        }

        public static Collection<ComponentType> getAll() {
            return KNOWN_COMPONENTS;
        }

        public static Collection<Identifier> getAllIds() {
            return ID.keySet();
        }

        public static Collection<Identifier> getAllAllowedIds() {
            var ids = new HashSet<>(ID.keySet());
            ids.removeAll(PolydexImpl.config.disabledHoverInformation);
            return ids;
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

        public enum Visibility {
            ALWAYS,
            SNEAKING,
            NEVER,
            @ApiStatus.Internal
            DEFAULT
        }
    }
}
