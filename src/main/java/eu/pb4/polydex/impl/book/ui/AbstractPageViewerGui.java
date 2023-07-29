package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractPageViewerGui extends ExtendedGui implements PageAware {
    public static final int PAGE_SIZE = 9 * 5;

    protected final Runnable closeCallback;
    private final LayerBuilder displayLayer;
    protected int page = 0;
    protected List<PolydexPage> pages;
    @Nullable
    protected PolydexEntry entry;

    public AbstractPageViewerGui(ServerPlayerEntity player, @Nullable Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X6, player, true);
        this.closeCallback = closeCallback;
        this.displayLayer = new LayerBuilder(player);
        this.addLayer(this.displayLayer, 0, 0);
        this.setOverlayTexture(InternalPageTextures.MAIN);
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
        this.setSlot(PAGE_SIZE, pageEntry.typeIcon(this.getPlayer()));
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
}
