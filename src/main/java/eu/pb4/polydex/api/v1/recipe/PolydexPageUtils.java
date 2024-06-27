package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.ui.PageViewerGui;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PolydexPageUtils {
    public static Event<Runnable> BEFORE_LOADING = EventFactory.createArrayBacked(Runnable.class, x -> () -> {
        for (var r : x) {
            r.run();
        }
    });

    public static Event<Runnable> AFTER_LOADING = EventFactory.createArrayBacked(Runnable.class, x -> () -> {
        for (var r : x) {
            r.run();
        }
    });

    public static Identifier identifierFromRecipe(Identifier identifier) {
        return identifier.withPrefixedPath("recipe/");
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

    public static List<PolydexEntry> getAllEntries() {
        return getAllEntries(true);
    }
    public static List<PolydexEntry> getAllEntries(boolean withEmpty) {
        return PolydexImpl.ITEM_ENTRIES.get(withEmpty);
    }

    @Nullable
    public static PolydexEntry getItemEntryFor(ItemStack stack) {
        return PolydexImpl.getEntry(stack);
    }

    @Nullable
    public static List<PolydexPage> getPagesForCategory(PolydexCategory category) {
        return PolydexImpl.CATEGORY_TO_PAGES.getOrDefault(category, List.of());
    }

    public static boolean openCategoryUi(ServerPlayerEntity player, PolydexCategory category, @Nullable Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        var entry = getPagesForCategory(category);
        if (entry.isEmpty()) {
            return false;
        }

        List<PolydexPage> list = new ArrayList<>();
        for (PolydexPage polydexPage : entry) {
            if (polydexPage.canDisplay(null, player)) {
                list.add(polydexPage);
            }
        }

        if (!entry.isEmpty()) {
            PageViewerGui.openCategory(player, category, list, closeCallback);
            return true;
        }


        return false;
    }

    @Nullable
    public static PolydexPage getPageById(Identifier identifier) {
        if (!isReady()) {
            return null;
        }
        return PolydexImpl.ID_TO_PAGE.get(identifier);
    }

    public static boolean openRecipeListUi(ServerPlayerEntity player, ItemStack stack, @Nullable Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        var entry = getItemEntryFor(stack);
        if (entry == null) {
            return false;
        }

        if (entry.getVisiblePagesSize(player) > 0) {
            PageViewerGui.openEntry(player, entry, false, closeCallback);
            return true;
        }

        return false;
    }

    public static boolean openRecipeListUi(ServerPlayerEntity player, PolydexEntry entry, @Nullable Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        if (entry.getVisiblePagesSize(player) > 0) {
            PageViewerGui.openEntry(player, entry, false, closeCallback);
            return true;
        }

        return false;
    }

    public static boolean openUsagesListUi(ServerPlayerEntity player, ItemStack stack, @Nullable  Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        var entry = getItemEntryFor(stack);
        if (entry == null) {
            return false;
        }

        if (entry.getVisibleIngredientPagesSize(player) > 0) {
            PageViewerGui.openEntry(player, entry, true, closeCallback);
            return true;
        }

        return false;
    }

    public static boolean openUsagesListUi(ServerPlayerEntity player, PolydexEntry entry, @Nullable  Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        if (entry.getVisibleIngredientPagesSize(player) > 0) {
            PageViewerGui.openEntry(player, entry, true, closeCallback);
            return true;
        }

        return false;
    }

    public static boolean openCustomPageUi(ServerPlayerEntity player, Text text, List<PolydexPage> pages, boolean useTypeIcon, @Nullable Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }


        if (pages.size() > 0) {
            PageViewerGui.openCustom(player, text, pages, useTypeIcon, closeCallback);
            return true;
        }

        return false;
    }
}
