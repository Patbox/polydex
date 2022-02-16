package eu.pb4.polydex.impl.book;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public abstract class PagedLayer extends Layer implements PageAware {
    private final boolean withNavigation;
    public final int pageSize;
    public final ServerPlayerEntity player;
    protected int page = 0;

    public PagedLayer(ServerPlayerEntity player, int height, int width, boolean withNavigation) {
        super(height, width);
        this.withNavigation = withNavigation;
        this.player = player;
        this.pageSize = this.withNavigation ?  (height - 1) * width : height * width;
    }


    protected void updateDisplay() {
        var offset = this.page * this.pageSize;

        for (int i = 0; i < this.pageSize; i++) {
            var element = this.getElement(offset + i);

            if (element == null) {
                element = GuiUtils.EMPTY;
            }

            this.setSlot(i, element);
        }

        for (int i = 0; i < this.width; i++) {
            var navElement = this.getNavElement(i);

            if (navElement == null) {
                navElement = GuiUtils.EMPTY;
            }

            this.setSlot(i + this.pageSize, navElement);
        }
    }

    public int getPage() {
        return this.page;
    }

    @Override
    public void setPage(int page) {
        this.page = Math.min(page, this.getPageAmount() - 1);
        this.updateDisplay();
    }

    protected abstract GuiElement getElement(int id);

    protected GuiElement getNavElement(int id) {
        return switch (id) {
            case 2 -> this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : GuiUtils.FILLER;
            case 4 -> this.getPageAmount() > 1 ? new GuiElementBuilder(Items.BOOK)
                    .setName(new TranslatableText("text.polydex.view.pages",
                                    new LiteralText("" + (this.page + 1)).formatted(Formatting.WHITE),
                                    new LiteralText("" + this.getPageAmount()).formatted(Formatting.WHITE)
                            ).formatted(Formatting.AQUA)
                    ).build() : GuiUtils.FILLER;
            case 6 -> this.getPageAmount() > 1 ? GuiUtils.nextPage(player, this) : GuiUtils.FILLER;
            default -> GuiUtils.FILLER;
        };
    }
}
