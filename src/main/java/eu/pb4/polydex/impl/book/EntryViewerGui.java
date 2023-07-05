package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.api.recipe.PageData;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import eu.pb4.sgui.api.gui.layered.LayeredGui;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class EntryViewerGui extends LayeredGui implements PageAware {
    public static final int PAGE_SIZE = 9 * 5;

    protected final Runnable closeCallback;
    private final LayerBuilder displayLayer;
    private final ItemEntry entry;
    private final List<PageData<?>> pages;
    private final boolean ingredientsView;
    protected int page = 0;

    public EntryViewerGui(ServerPlayerEntity player, ItemEntry entry, boolean ingredients, @Nullable Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X6, player, true);
        this.closeCallback = closeCallback;
        this.entry = entry;
        this.ingredientsView = ingredients;
        this.pages = ingredients ? entry.getVisibleIngredientPages(player) : entry.getVisiblePages(player);
        this.displayLayer = new LayerBuilder(player, entry);
        this.addLayer(this.displayLayer, 0, 0);
        this.setTitle(Text.translatable(ingredients ? "text.polydex.recipes_title_input" : "text.polydex.recipes_title_output", this.entry.stack().getName()));

        var filler = GuiUtils.FILLER;

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
        this.updateDisplay();
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
        var pageEntry = this.pages.get(this.page);
        this.displayLayer.clear();
        pageEntry.createPage(this.entry, this.displayLayer, this.getPlayer());
        this.setSlot(PAGE_SIZE, pageEntry.getIcon(this.entry, this.getPlayer()));
        if (this.pages.size() > 1) {
            this.setSlot(PAGE_SIZE + 4, new GuiElementBuilder(Items.BOOK)
                    .setName(
                            Text.translatable("text.polydex.view.pages",
                                    Text.literal("" + (this.page + 1)).formatted(Formatting.WHITE),
                                    Text.literal("" + this.getPageAmount()).formatted(Formatting.WHITE)
                            ).formatted(Formatting.AQUA)
                    )
            );
        }
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
}
