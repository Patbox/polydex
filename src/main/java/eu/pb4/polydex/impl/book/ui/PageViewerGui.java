package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexCategory;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public class PageViewerGui extends ExtendedGui implements PageAware {
    public static final int PAGE_SIZE = 9 * 5;

    protected final Runnable closeCallback;
    private final LayerBuilder displayLayer;
    private final SubRecipeLayer subPages;
    private final IconGetter iconGetter;
    protected int page = 0;
    protected final List<PolydexPage> pages;
    @Nullable
    protected final PolydexEntry entry;

    public PageViewerGui(ServerPlayerEntity player, Text title, @Nullable PolydexEntry entry, List<PolydexPage> pages, IconGetter iconGetter, @Nullable Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X6, player, true);
        this.closeCallback = closeCallback;
        this.iconGetter = iconGetter;
        this.displayLayer = new LayerBuilder(player);
        this.pages = pages;
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

    public static void openCustom(ServerPlayerEntity player, Text title, List<PolydexPage> pages, boolean useTypeIcon, @Nullable Runnable closeCallback) {
        new PageViewerGui(player, title, null, pages, useTypeIcon ? PolydexPage::typeIcon : PolydexPage::entryIcon, closeCallback);
    }

    public static void openCategory(ServerPlayerEntity player, PolydexCategory category, List<PolydexPage> pages, @Nullable Runnable closeCallback) {
        var title = Text.translatable("text.polydex.recipes_title_category", category.name());
        new PageViewerGui(player, title, null, pages, PolydexPage::entryIcon, closeCallback);
    }

    public static void openEntry(ServerPlayerEntity player, PolydexEntry entry, boolean ingredients, @Nullable Runnable closeCallback) {
        var pages = ingredients ? entry.getVisibleIngredientPages(player) : entry.getVisiblePages(player);
        var title = Text.translatable(ingredients ? "text.polydex.recipes_title_input" : "text.polydex.recipes_title_output", entry.stack().getName());
        new PageViewerGui(player, title, entry, pages, PolydexPage::typeIcon, closeCallback);
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
            this.setSlot(PAGE_SIZE + 4, GuiUtils.page(this.getPlayer(),  this.page + 1, this.getPageAmount()));
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

    public class SubRecipeLayer extends PagedLayer {
        public SubRecipeLayer(int height) {
            super(PageViewerGui.this.getPlayer(), height, 9, true);
        }


        @Override
        protected int getEntryCount() {
            return pages.size();
        }

        @Override
        protected GuiElement getElement(int id) {
            if (id < pages.size()) {
                var page = pages.get(id);

                return GuiElementBuilder.from(iconGetter.getIcon(page, entry, player))
                        .setCallback((x, type, z) -> {
                            PageViewerGui.this.setPage(id);
                            GuiUtils.playClickSound(this.player);
                        })
                        .build();

            }
            return GuiElement.EMPTY;
        }
    }

    @FunctionalInterface
    public interface IconGetter {
        ItemStack getIcon(PolydexPage polydexPage, @Nullable PolydexEntry entry, ServerPlayerEntity player);
    }
}
