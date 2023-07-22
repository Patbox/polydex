package eu.pb4.polydex.api.v1;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.EntryViewerGui;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PolydexUtils {
    public static final Text DEFAULT_SEPARATOR = Text.literal(" | ").formatted(Formatting.DARK_GRAY);
    public static final Text SPACE_SEPARATOR = Text.literal(" ");

    public static Identifier fromRecipe(Recipe<?> recipe) {
        return recipe.getId().withPrefixedPath("recipe/");
    }

    public static Text mergeText(Collection<Text> texts, Text separator) {
        var out = Text.literal("");

        var iterator = texts.iterator();

        while (iterator.hasNext()) {
            out.append(iterator.next());

            if (iterator.hasNext()) {
                out.append(separator);
            }
        }

        return out;
    }

    public static Text mergeText(Collection<Text> texts) {
        var out = Text.literal("");

        var iterator = texts.iterator();

        if (iterator.hasNext()) {
            out.append(iterator.next());
        }

        return out;
    }

    public static Text createText(ItemStack stack) {
        if (stack.isEmpty()) {
            return Text.translatable("text.polydex.empty");
        } else if (stack.getCount() == 1) {
            return stack.getName();
        } else {
            return Text.literal("" + stack.getCount() + " × ").append(stack.getName());
        }
    }

    public static boolean isReady() {
        return PolydexImpl.isReady();
    }

    @Nullable
    public static PolydexEntry getItemEntryFor(ItemStack stack) {
        return PolydexImpl.getEntry(stack);
    }

    public static boolean openRecipeListUi(ServerPlayerEntity player, ItemStack stack, Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        var entry = getItemEntryFor(stack);
        if (entry == null) {
            return false;
        }

        if (entry.getVisiblePagesSize(player) > 0) {
            new EntryViewerGui(player, entry, false, closeCallback).open();
            return true;
        }

        return false;
    }

    public static boolean openUsagesListUi(ServerPlayerEntity player, ItemStack stack, Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        var entry = getItemEntryFor(stack);
        if (entry == null) {
            return false;
        }

        if (entry.getVisibleIngredientPagesSize(player) > 0) {
            new EntryViewerGui(player, entry, true, closeCallback).open();
            return true;
        }

        return false;
    }
}
