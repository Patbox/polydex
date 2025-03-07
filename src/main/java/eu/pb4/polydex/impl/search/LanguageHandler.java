package eu.pb4.polydex.impl.search;

import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import xyz.nucleoid.server.translations.api.LocalizationTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class LanguageHandler {
    public static List<String> toTranslatedString(List<Text> texts, ServerPlayerEntity player) {
        var arr = new ArrayList<String>(texts.size());
        for (var text : texts) {
            arr.add(stringifyText(text, player).toLowerCase(Locale.ROOT));
        }
        return arr;
    }

    public static String stringifyText(Text input, ServerPlayerEntity player) {
        var code = LocalizationTarget.of(player).getLanguageCode();
        if (!PolydexImpl.config.enableLanguageSearch || code == null || code.equals(LocalizationTarget.ofSystem().getLanguageCode())) {
            return input.getString();
        }

        var b = new StringBuilder();
        visitText(input, (s) -> {
            b.append(s);
            return Optional.empty();
        });
        return b.toString();
    }

    private static void visitText(Text text, StringVisitable.Visitor<?> visitor) {
        if (text.getContent() instanceof TranslatableTextContent content) {
            // Todo
            content.visit(visitor);
        } else {
            text.getContent().visit(visitor);
        }
        for (var s : text.getSiblings()) {
            visitText(s, visitor);
        }
    }
}
