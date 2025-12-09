package eu.pb4.polydex.impl.search;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.mixin.TranslatableContentsAccessor;
import xyz.nucleoid.server.translations.api.LocalizationTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;

public class TextTranslationUtils {
    public static List<String> toTranslatedString(List<Component> texts, ServerPlayer player) {
        var arr = new ArrayList<String>(texts.size());
        for (var text : texts) {
            arr.add(stringifyText(text, player).toLowerCase(Locale.ROOT));
        }
        return arr;
    }

    public static String stringifyText(Component input, ServerPlayer player) {
        var code = LocalizationTarget.of(player).getLanguageCode();
        if (!PolydexImpl.config.enableLanguageSearch || code == null || code.equals(LocalizationTarget.ofSystem().getLanguageCode())) {
            return input.getString();
        }

        var b = new StringBuilder();
        var lang = LanguageHandler.get(code);
        visitText(lang, input, (s) -> {
            b.append(s);
            return Optional.empty();
        });
        return b.toString();
    }

    private static void visitText(LanguageHandler lang, Component text, FormattedText.ContentConsumer<?> visitor) {
        if (text.getContents() instanceof TranslatableContents content) {
            var translation = lang.get(content.getKey(), content.getFallback());
            if (content.getArgs().length == 0) {
                visitor.accept(translation);
            } else {
                ((TranslatableContentsAccessor) content).callDecomposeTemplate(translation, stringVisitable -> {
                    if (stringVisitable instanceof Component text1) {
                        visitText(lang, text1, visitor);
                    } else {
                        stringVisitable.visit(visitor);
                    }
                });
            }
        } else {
            text.getContents().visit(visitor);
        }
        for (var s : text.getSiblings()) {
            visitText(lang, s, visitor);
        }
    }
}
