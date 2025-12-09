package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import net.minecraft.resources.Identifier;

public record PolydexEntryImpl(Identifier identifier, PolydexStack<?> stack, List<PolydexPage> outputPages, List<PolydexPage> ingredientPages, BiPredicate<PolydexEntry, PolydexStack<?>> isPartOf) implements PolydexEntry {
    public static final BiPredicate<PolydexEntry, PolydexStack<?>> WEAK_CHECK = ((itemEntry, stack1) -> stack1.matches(itemEntry.stack(), false));
    public static final BiPredicate<PolydexEntry, PolydexStack<?>> STRICT_CHECK = ((itemEntry, stack1) -> stack1.matches(itemEntry.stack(), true));

    public boolean isPartOf(PolydexStack<?> stack) {
        return this.isPartOf.test(this, stack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolydexEntryImpl that = (PolydexEntryImpl) o;
        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.identifier);
    }
}
