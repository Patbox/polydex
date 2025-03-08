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
                curr = parser.peek();
                String token;
                if (curr == '"') {
                    token = parser.readQuotedString();
                } else {
                    int i = 0;
                    for (; i < parser.getRemainingLength(); i++) {
                        if (parser.peek(i) == ' ') {
                            break;
                        }
                    }
                    token =  input.substring(parser.getCursor(), parser.getCursor() + i);
                    parser.setCursor(parser.getCursor() + i);
                }

                query.words.add(new QueryValue<>(token.toLowerCase(Locale.ROOT), value));
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
            boolean match = false;

            for (var x : this.namespaces) {
                if (id.getNamespace().startsWith(x.searched) == x.value) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
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
            var texts = TextTranslationUtils.toTranslatedString(stack.getTexts(player), player);


            for (var x : this.words) {
                boolean match = false;
                for (var text : texts) {
                    if (text.contains(x.searched) == x.value) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return false;
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
