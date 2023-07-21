package eu.pb4.polydex.impl.book;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public abstract class PagedLayer extends Layer implements PageAware {
    public final int pageSize;
    public final ServerPlayerEntity player;
    private final boolean withNavigation;
    protected int page = 0;

    public PagedLayer(ServerPlayerEntity player, int height, int width, boolean withNavigation) {
        super(height, width);
        this.withNavigation = withNavigation;
        this.player = player;
        this.pageSize = this.withNavigation ? (height - 1) * width : height * width;
    }


    protected void updateDisplay() {
        var offset = this.page * this.pageSize;

        for (int i = 0; i < this.pageSize; i++) {
            var element = this.getElement(offset + i);

            if (element == null) {
                element = GuiElement.EMPTY;
            }

            this.setSlot(i, element);
        }

        for (int i = 0; i < this.width; i++) {
            var navElement = this.getNavElement(i);

            if (navElement == null) {
                navElement = GuiElement.EMPTY;
            }

            this.setSlot(i + this.pageSize, navElement);
        }
    }

    private GuiElement filler() {
        return PolymerResourcePackUtils.hasPack(this.player) ? GuiElement.EMPTY : GuiUtils.FILLER;
    }

    public int getPage() {
        return this.page;
    }

    @Override
    public void setPage(int page) {
        this.page = page % Math.max(this.getPageAmount(), 1);
        this.updateDisplay();
    }

    protected abstract GuiElement getElement(int id);

    protected GuiElement getNavElement(int id) {
        return switch (id) {
            case 2 -> this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : filler();
            case 4 -> this.getPageAmount() > 1 ? new GuiElementBuilder(Items.BOOK)
                    .setName(Text.translatable("text.polydex.view.recipeOutput",
                                    Text.literal("" + (this.page + 1)).formatted(Formatting.WHITE),
                                    Text.literal("" + this.getPageAmount()).formatted(Formatting.WHITE)
                            ).formatted(Formatting.AQUA)
                    ).build() : GuiUtils.FILLER;
            case 6 -> this.getPageAmount() > 1 ? GuiUtils.nextPage(player, this) : filler();
            default -> GuiUtils.FILLER;
        };
    }
}
