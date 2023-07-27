package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.ui.EntryPageViewerGui;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PolydexPageUtils {

    public static Identifier identifierFromRecipe(Recipe<?> recipe) {
        return recipe.getId().withPrefixedPath("recipe/");
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

    @Nullable
    public static List<PolydexPage> getPagesForCategory(PolydexCategory category) {
        return PolydexImpl.CATEGORY_TO_PAGES.getOrDefault(category, List.of());
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
            new EntryPageViewerGui(player, entry, false, closeCallback).open();
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
            new EntryPageViewerGui(player, entry, true, closeCallback).open();
            return true;
        }

        return false;
    }
}
