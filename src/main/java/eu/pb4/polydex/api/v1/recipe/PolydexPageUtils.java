package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.ui.PageViewerGui;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class PolydexPageUtils {
    private static final DecimalFormat CHANCE_FORMAT = Util.make(new DecimalFormat("#.###"), df -> df.setRoundingMode(RoundingMode.HALF_UP));
    public static Event<Consumer<MinecraftServer>> BEFORE_PAGE_LOADING = EventFactory.createArrayBacked(Consumer.class, x -> (server) -> {
        for (var r : x) {
            r.accept(server);
        }
    });

    public static Event<Consumer<MinecraftServer>> AFTER_PAGE_LOADING = EventFactory.createArrayBacked(Consumer.class, x -> (server) -> {
        for (var r : x) {
            r.accept(server);
        }
    });

    public static Identifier identifierFromRecipe(Identifier identifier) {
        return identifier.withPrefix("recipe/");
    }

    public static Component createText(ItemStack stack) {
        if (stack.isEmpty()) {
            return Component.translatable("text.polydex.empty");
        } else if (stack.getCount() == 1) {
            return stack.getHoverName();
        } else {
            return Component.literal(stack.getCount() + " × ").append(stack.getHoverName());
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

    public static Collection<PolydexPage> getAllPages() {
        return PolydexImpl.ID_TO_PAGE.values();
    }

    @Nullable
    public static PolydexEntry getItemEntryFor(ItemStack stack) {
        return PolydexImpl.getEntry(stack);
    }
    @Nullable
    public static PolydexEntry getEntry(Identifier identifier) {
        return PolydexImpl.ITEM_ENTRIES.byId().get(identifier);
    }

    @Nullable
    public static List<PolydexPage> getPagesForCategory(PolydexCategory category) {
        return PolydexImpl.CATEGORY_TO_PAGES.getOrDefault(category, List.of());
    }

    public static boolean openCategoryUi(ServerPlayer player, PolydexCategory category, @Nullable Runnable closeCallback) {
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

    public static boolean openRecipeListUi(ServerPlayer player, ItemStack stack, @Nullable Runnable closeCallback) {
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

    public static boolean openRecipeListUi(ServerPlayer player, PolydexStack<?> stack, @Nullable Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        var entry = PolydexImpl.STACK_TO_ENTRY.get(stack);
        if (entry == null) {
            return false;
        }

        if (entry.getVisiblePagesSize(player) > 0) {
            PageViewerGui.openEntry(player, entry, false, closeCallback);
            return true;
        }

        return false;
    }

    public static boolean openRecipeListUi(ServerPlayer player, PolydexEntry entry, @Nullable Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        if (entry.getVisiblePagesSize(player) > 0) {
            PageViewerGui.openEntry(player, entry, false, closeCallback);
            return true;
        }

        return false;
    }

    public static boolean openUsagesListUi(ServerPlayer player, ItemStack stack, @Nullable  Runnable closeCallback) {
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

    public static boolean openUsagesListUi(ServerPlayer player, PolydexStack<?> stack, @Nullable  Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        var entry = PolydexImpl.STACK_TO_ENTRY.get(stack);
        if (entry == null) {
            return false;
        }

        if (entry.getVisibleIngredientPagesSize(player) > 0) {
            PageViewerGui.openEntry(player, entry, true, closeCallback);
            return true;
        }

        return false;
    }

    public static boolean openUsagesListUi(ServerPlayer player, PolydexEntry entry, @Nullable  Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }

        if (entry.getVisibleIngredientPagesSize(player) > 0) {
            PageViewerGui.openEntry(player, entry, true, closeCallback);
            return true;
        }

        return false;
    }

    public static boolean openCustomPageUi(ServerPlayer player, Component text, List<PolydexPage> pages, boolean useTypeIcon, @Nullable Runnable closeCallback) {
        if (!isReady()) {
            return false;
        }


        if (!pages.isEmpty()) {
            PageViewerGui.openCustom(player, text, pages, useTypeIcon, closeCallback);
            return true;
        }

        return false;
    }


    @Deprecated
    public static Event<Runnable> BEFORE_LOADING = EventFactory.createArrayBacked(Runnable.class, x -> () -> {
        for (var r : x) {
            r.run();
        }
    });
    @Deprecated
    public static Event<Runnable> AFTER_LOADING = EventFactory.createArrayBacked(Runnable.class, x -> () -> {
        for (var r : x) {
            r.run();
        }
    });


    static {
        BEFORE_PAGE_LOADING.register((s) -> BEFORE_LOADING.invoker().run());
        AFTER_PAGE_LOADING.register((s) -> AFTER_LOADING.invoker().run());
    }

    public static String formatChanceAmount(float chance) {
        return CHANCE_FORMAT.format(chance * 100) + "%";
    }
}
