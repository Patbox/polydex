package eu.pb4.polydex.impl.search;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record SearchResult(SearchQuery query, List<PolydexEntry> all, List<PolydexEntry> nonEmpty) {
    public static final SearchResult EMPTY = new SearchResult(SearchQuery.EMPTY, List.of(), List.of());

    public static SearchResult global() {
        return new SearchResult(SearchQuery.EMPTY, PolydexImpl.ITEM_ENTRIES.all(), PolydexImpl.ITEM_ENTRIES.nonEmpty());
    }

    public static CompletableFuture<SearchResult> getAsync(SearchQuery query, ServerPlayerEntity player) {
        if (query.isEmpty()) {
            return CompletableFuture.completedFuture(global());
        }

        return CompletableFuture.supplyAsync(() -> get(query, player));
    }

    public static SearchResult get(SearchQuery query, ServerPlayerEntity player) {
        if (!query.isEmpty()) {
            var all = new ArrayList<PolydexEntry>();
            var nonEmpty = new ArrayList<PolydexEntry>();
            for (var entry : PolydexImpl.ITEM_ENTRIES.all()) {
                if (query.test(entry.stack(), player)) {
                    all.add(entry);
                    if (entry.hasPages()) {
                        nonEmpty.add(entry);
                    }
                }
            }
            return new SearchResult(query, all, nonEmpty);
        } else {
            return global();
        }
    }

    public List<PolydexEntry> get(boolean showAll) {
        return showAll ? all : nonEmpty;
    }
}
