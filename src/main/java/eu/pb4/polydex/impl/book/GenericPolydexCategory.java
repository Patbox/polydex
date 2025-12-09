package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.v1.recipe.PolydexCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;

public record GenericPolydexCategory(Identifier identifier, Component name) implements PolydexCategory {
    private static final Map<Identifier, GenericPolydexCategory> INSTANCES = new HashMap<>();

    public static PolydexCategory of(Identifier identifier) {
        return INSTANCES.computeIfAbsent(identifier, GenericPolydexCategory::create);
    }

    private static GenericPolydexCategory create(Identifier identifier) {
        return new GenericPolydexCategory(
                identifier,
                Component.translatable(Util.makeDescriptionId("polydex_category", identifier))
        );
    }
}
