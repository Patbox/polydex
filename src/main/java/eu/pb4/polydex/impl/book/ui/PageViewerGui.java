package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexCategory;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.impl.PlayerInterface;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.AnimatedGuiElement;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class PageViewerGui extends ExtendedGui implements PageAware {
    public static final int PAGE_SIZE = 9 * 5;

    protected final Runnable closeCallback;
    protected final List<PolydexPage> pages;
    protected final List<GroupedPages> groupedPages;
    @Nullable
    protected final PolydexEntry entry;
    private final LayerBuilder displayLayer;
    private final SubRecipeLayer subPages;
    private final IconGetter iconGetter;
    protected int page = 0;

    public PageViewerGui(ServerPlayer player, Component title, @Nullable PolydexEntry entry, List<PolydexPage> pages, IconGetter iconGetter, @Nullable Runnable closeCallback) {
        super(MenuType.GENERIC_9x6, player, true);
        this.closeCallback = closeCallback;
        this.iconGetter = iconGetter;
        this.displayLayer = new LayerBuilder(player);
        this.pages = pages;
        this.groupedPages = GroupedPages.of(this.pages);
        this.entry = entry;
        this.addLayer(this.displayLayer, 0, 0);
        this.setOverlayTexture(InternalPageTextures.MAIN);
        this.setText(title);
        this.subPages = new SubRecipeLayer(4);
        this.addLayer(this.subPages, 0, 6);
        this.setupNavigator();
        this.updateDisplay();
        this.subPages.updateDisplay();
        this.open();
    }

    public static void openCustom(ServerPlayer player, Component title, List<PolydexPage> pages, boolean useTypeIcon, @Nullable Runnable closeCallback) {
        new PageViewerGui(player, title, null, pages, useTypeIcon ? PolydexPage::typeIcon : PolydexPage::entryIcon, closeCallback);
    }

    public static void openCategory(ServerPlayer player, PolydexCategory category, List<PolydexPage> pages, @Nullable Runnable closeCallback) {
        var title = Component.translatable("text.polydex.recipes_title_category", category.name());
        new PageViewerGui(player, title, null, pages, PolydexPage::entryIcon, closeCallback);
    }

    public static void openEntry(ServerPlayer player, PolydexEntry entry, boolean ingredients, @Nullable Runnable closeCallback) {
        PlayerInterface.addViewed(player, entry.identifier());
        var pages = ingredients ? entry.getVisibleIngredientPages(player) : entry.getVisiblePages(player);
        var title = Component.translatable(ingredients ? "text.polydex.recipes_title_input" : "text.polydex.recipes_title_output", entry.stack().getName());
        new PageViewerGui(player, title, entry, pages, ingredients ? PolydexPage::entryIcon : PolydexPage::typeIcon, closeCallback);
    }

    protected void setupNavigator() {
        var filler = filler();

        boolean addNav = this.pages.size() > 1;
        this.setSlot(PAGE_SIZE + 1, filler);
        this.setSlot(PAGE_SIZE + 2, filler);
        this.setSlot(PAGE_SIZE + 3, addNav ? GuiUtils.previousPage(this.getPlayer(), this) : filler);
        this.setSlot(PAGE_SIZE + 4, filler);
        this.setSlot(PAGE_SIZE + 5, addNav ? GuiUtils.nextPage(this.getPlayer(), this) : filler);
        this.setSlot(PAGE_SIZE + 6, filler);
        this.setSlot(PAGE_SIZE + 7, filler);
        this.setSlot(PAGE_SIZE + 8, GuiUtils.backButton(this.getPlayer(), () -> {
            if (this.closeCallback != null) {
                this.closeCallback.run();
            } else {
                this.close();
            }
        }, this.closeCallback != null));
    }

    @Override
    public void onTick() {
        if (!PolydexImpl.isReady()) {
            this.close();
            return;
        }
        super.onTick();
    }

    protected void updateDisplay() {
        if (!PolydexImpl.isReady()) {
            return;
        }
        this.lock();
        var pageEntry = this.pages.get(this.page);
        var t = pageEntry.texture(this.getPlayer());
        this.setTexture(t);
        this.displayLayer.clear(t != null ? filler() : GuiUtils.FILLER);
        pageEntry.createPage(this.entry, this.getPlayer(), this.displayLayer);
        this.setSlot(PAGE_SIZE, pageEntry.typeIcon(this.entry, this.getPlayer()));
        if (this.pages.size() > 1) {
            this.setSlot(PAGE_SIZE + 4, GuiUtils.page(this.getPlayer(), this.page + 1, this.getPageAmount()));
        }
        this.unlock();
    }

    public int getPage() {
        return this.page;
    }

    @Override
    public void setPage(int i) {
        this.page = i;
        this.updateDisplay();
    }

    @Override
    public int getPageAmount() {
        return this.pages.size();
    }

    @FunctionalInterface
    public interface IconGetter {
        ItemStack getIcon(PolydexPage polydexPage, @Nullable PolydexEntry entry, ServerPlayer player);
    }

    public record GroupedPages(String group, List<PolydexPage> pages, int index) {
        public static List<GroupedPages> of(List<PolydexPage> pages) {
            var out = new ArrayList<GroupedPages>();
            GroupedPages group = null;

            for (int i = 0; i < pages.size(); i++) {
                var page = pages.get(i);

                if (page.getGroup().isEmpty()) {
                    if (group != null) {
                        out.add(group);
                        group = null;
                    }
                    out.add(new GroupedPages("", List.of(page), i));
                    continue;
                }

                if (group == null) {
                    group = new GroupedPages(page.getGroup(), new ArrayList<>(), i);
                    group.pages.add(page);
                } else if (group.group.equals(page.getGroup())) {
                    group.pages.add(page);
                } else {
                    out.add(group);
                    group = new GroupedPages(page.getGroup(), new ArrayList<>(), i);
                    group.pages.add(page);
                }
            }

            if (group != null) {
                out.add(group);
            }

            return out;
        }
    }

    public class SubRecipeLayer extends PagedLayer {
        public SubRecipeLayer(int height) {
            super(PageViewerGui.this.getPlayer(), height, 9, true);
        }


        @Override
        protected int getEntryCount() {
            return groupedPages.size();
        }

        @Override
        protected GuiElementInterface getElement(int id) {
            if (id < groupedPages.size()) {
                var group = groupedPages.get(id);

                var list = new ArrayList<>(group.pages.size());
                for (var page : group.pages) {
                    list.add(iconGetter.getIcon(page, entry, player));
                }

                return new AnimatedGuiElement(list.toArray(new ItemStack[0]), 10, false, (x, type, z) -> {
                    PageViewerGui.this.setPage(group.index);
                    GuiUtils.playClickSound(this.player);
                });
            }
            return GuiElement.EMPTY;
        }
    }
}
