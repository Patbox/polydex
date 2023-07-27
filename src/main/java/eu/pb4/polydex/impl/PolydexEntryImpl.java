package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

public record PolydexEntryImpl(Identifier identifier, PolydexStack<?> stack, List<PolydexPage> outputPages, List<PolydexPage> ingredientPages, BiPredicate<PolydexEntry, PolydexStack<?>> isPartOf) implements PolydexEntry {
    public static final BiPredicate<PolydexEntry, PolydexStack<?>> WEAK_CHECK = ((itemEntry, stack1) -> stack1.matches(itemEntry.stack(), false));
    public static final BiPredicate<PolydexEntry, PolydexStack<?>> STRICT_CHECK = ((itemEntry, stack1) -> stack1.matches(itemEntry.stack(), true));

    public boolean isPartOf(PolydexStack<?> stack) {
        return this.isPartOf.test(this, stack);
    }
}
