package eu.pb4.polydex.impl.search;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public record SearchQuery(String input, Set<QueryValue<String>> namespaces,
                          Set<QueryValue<Pair<String, String>>> tags, Set<QueryValue<String>> words) {


    public static final SearchQuery EMPTY = new SearchQuery("", Set.of(), Set.of(), Set.of());

    public static SearchQuery parse(String input) throws CommandSyntaxException {
        var parser = new StringReader(input.toLowerCase(Locale.ROOT));
        var query = new SearchQuery(input, new HashSet<>(), new HashSet<>(), new HashSet<>());

        while (parser.canRead()) {
            parser.skipWhitespace();
            if (!parser.canRead()) {
                break;
            }
            var curr = parser.peek();
            var value = true;

            if (curr == '!') {
                value = false;
                parser.skipWhitespace();
                if (!parser.canRead(2)) {
                    break;
                }
                parser.skip();
                curr = parser.peek();
                parser.skipWhitespace();
            }

            if (curr == '#' || curr == '@') {
                if (!parser.canRead()) {
                    break;
                } else {
                    parser.skip();
                }
            }
            if (curr == '@') {
                var token = parser.readString();
                if (token.isEmpty()) {
                    continue;
                }
                query.namespaces.add(new QueryValue<>(token, value));
            } else if (curr == '#') {
                try {
                    var x = parser.getCursor();
                    var id = Identifier.fromCommandInput(parser);
                    var namespace = input.substring(x, parser.getCursor()).contains(":") ? id.getNamespace() : "";
                    query.tags.add(new QueryValue<>(new Pair<>(namespace, id.getPath()), value));
                } catch (Throwable e) {
                }
            } else {
                var token = parser.readString();
                if (token.isEmpty()) {
                    break;
                }
                query.words.add(new QueryValue<>(token, value));
            }
        }

        return query;
    }

    public boolean test(PolydexStack<?> stack, ServerPlayerEntity player) {
        if (!this.namespaces.isEmpty()) {
            var id = stack.getId();
            if (id == null) {
                return false;
            }
            for (var x : this.namespaces) {
                if (id.getNamespace().startsWith(x.searched) == x.value) {
                    return true;
                }
            }
            return false;
        }

        if (!this.tags.isEmpty()) {
            var tags = stack.streamTags().map(TagKey::id).collect(Collectors.toSet());
            if (tags.isEmpty()) {
                return false;
            }
            for (var x : this.tags) {
                var match = false;
                for (var y : tags) {
                    if ((y.getNamespace().contains(x.searched.getLeft()) && y.getPath().contains(x.searched.getRight())) == x.value) {
                        match = true;
                        break;
                    }
                }

                if (!match) {
                    return false;
                }
            }
        }

        if (!this.words.isEmpty()) {
            var texts = LanguageHandler.toTranslatedString(stack.getTexts(player), player);

            for (var x : this.words) {
                for (var text : texts) {
                    if (text.contains(x.searched) != x.value) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean isEmpty() {
        return this.tags.isEmpty() && this.words.isEmpty() && this.namespaces().isEmpty();
    }


    public record QueryValue<T>(T searched, boolean value) {}
}
