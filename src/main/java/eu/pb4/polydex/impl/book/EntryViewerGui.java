package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import eu.pb4.sgui.api.gui.layered.LayeredGui;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public final class EntryViewerGui extends LayeredGui implements PageAware {
    public static final int PAGE_SIZE = 9 * 5;
    
    protected final Runnable closeCallback;
    private final Layer displayLayer;
    private final ItemEntry entry;
    protected int page = 0;

    public EntryViewerGui(ServerPlayerEntity player, ItemEntry entry, @Nullable Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X6, player, false);
        this.closeCallback = closeCallback;
        this.entry = entry;
        this.displayLayer = new Layer(5, 9);
        this.addLayer(this.displayLayer, 0, 0);
        this.setTitle(new TranslatableText("text.polydex.recipes_title", this.entry.stack().getName()));

        var filler = GuiUtils.FILLER;

        boolean addNav = entry.pages().size() > 1;
        this.setSlot(PAGE_SIZE + 1, filler);
        this.setSlot(PAGE_SIZE + 2, filler);
        this.setSlot(PAGE_SIZE + 3, addNav ? GuiUtils.previousPage(this.getPlayer(), this) : filler);
        this.setSlot(PAGE_SIZE + 4, filler);
        this.setSlot(PAGE_SIZE + 5, addNav ? GuiUtils.nextPage(this.getPlayer(), this) : filler);
        this.setSlot(PAGE_SIZE + 6, filler);
        this.setSlot(PAGE_SIZE + 7, filler);
        this.setSlot(PAGE_SIZE + 8, GuiUtils.backButton(this.getPlayer(), () -> {
            this.close();
            if (this.closeCallback != null) {
                this.closeCallback.run();
            }
        }, this.closeCallback != null));
        this.updateDisplay();
    }
    

    @Override
    public void onClose() {

    }

    protected void updateDisplay() {
        var pageEntry = this.entry.pages().get(this.page);
        GuiElement fill = new GuiElementBuilder(Items.BLACK_STAINED_GLASS_PANE).setName(new LiteralText("")).build();
        for (int i = 0, size = this.displayLayer.getSize(); i < size; i++) {
            this.displayLayer.setSlot(i, fill);
        }
        pageEntry.renderLayer(this.entry, this.displayLayer, this.getPlayer(), this::reopen);
        this.setSlot(PAGE_SIZE, pageEntry.getIcon(this.entry, this.getPlayer(), this::reopen));
        if (this.entry.pages().size() > 1) {
            this.setSlot(PAGE_SIZE + 4, new GuiElementBuilder(Items.BOOK)
                    .setName(
                            new TranslatableText("text.polydex.view.pages",
                                    new LiteralText("" + (this.page + 1)).formatted(Formatting.WHITE),
                                    new LiteralText("" + this.getPageAmount()).formatted(Formatting.WHITE)
                            ).formatted(Formatting.AQUA)
                    )
            );
        }
    }

    private void reopen() {
        new EntryViewerGui(this.getPlayer(), this.entry, this.closeCallback).open();
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
        return this.entry.pages().size();
    }
}
