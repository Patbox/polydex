package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

public record PolydexEntryImpl(Identifier identifier, PolydexStack<?> stack, List<PolydexPage> outputPages, List<PolydexPage> ingredientPages, BiPredicate<PolydexEntry, PolydexStack<?>> isPartOf) implements PolydexEntry {
    public static final BiPredicate<PolydexEntry, PolydexStack<?>> WEAK_CHECK = new WeakCheck();
    public static final BiPredicate<PolydexEntry, PolydexStack<?>> STRICT_CHECK = new StrictCheck();

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


    private record WeakCheck() implements BiPredicate<PolydexEntry, PolydexStack<?>> {
        @Override
        public boolean test(PolydexEntry entry, PolydexStack<?> polydexStack) {
            return polydexStack.matches(entry.stack(), false);
        }
    }

    private record StrictCheck() implements BiPredicate<PolydexEntry, PolydexStack<?>> {
        @Override
        public boolean test(PolydexEntry entry, PolydexStack<?> polydexStack) {
            return polydexStack.matches(entry.stack(), true);
        }
    }
}
