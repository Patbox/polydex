package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.LayeredGui;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class ExtendedGui extends LayeredGui {
    private static final Style INFO_STYLE = Style.EMPTY.withFont(new Identifier("uniform"));
    private static final Style TEXTURE_STYLE = Style.EMPTY.withFont(new Identifier("polydex:gui")).withColor(Formatting.WHITE);
    private Text text = Text.empty();
    private Text texture;
    private Text overlayTexture;
    private boolean lock = false;
    private boolean isDirty = false;

    public ExtendedGui(ScreenHandlerType<?> type, ServerPlayerEntity player, boolean manipulatePlayerSlots) {
        super(type, player, manipulatePlayerSlots);
    }
    
    public void setText(@Nullable Text text) {
        this.text = text;
        this.updateText();
    }
    
    
    public void setTexture(@Nullable Text text) {
        this.texture = text;
        this.updateText();
    }

    public void setOverlayTexture(@Nullable Text text) {
        this.overlayTexture = text;
        this.updateText();
    }

    public void lock() {
        this.lock = true;
    }

    public void unlock() {
        this.lock = false;
        if (this.isDirty) {
            this.updateText();
        }
    }

    private void updateText() {
        if (this.lock) {
            this.isDirty = true;
            return;
        }
        this.isDirty = false;
        if (PolymerResourcePackUtils.hasPack(this.getPlayer())) {
            var text = Text.empty();
            var textTexture = Text.empty().setStyle(TEXTURE_STYLE);

            if (this.texture != null) {
                textTexture.append("c").append(this.texture).append("d");
            }

            if (this.overlayTexture != null) {
                textTexture.append("a").append(this.overlayTexture).append("b");
            }

            if (!textTexture.getSiblings().isEmpty()) {
                text.append(textTexture);
            }

            if (this.text != null) {
                text.append(this.text);
            }
            super.setTitle(text);
        } else {
            super.setTitle(this.text != null ? this.text : Text.empty());
        }
    }


    @Deprecated
    @Override
    public void setTitle(Text title) {
        super.setTitle(title);
    }

    public GuiElement filler() {
        return PolymerResourcePackUtils.hasPack(this.getPlayer()) ? GuiElement.EMPTY : GuiUtils.FILLER;
    }
}
