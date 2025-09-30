package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.LayeredGui;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class ExtendedGui extends LayeredGui {
    private static final Style INFO_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("uniform")));
    public static final Style TEXTURE_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("polydex:gui"))).withColor(Formatting.WHITE);
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
        super.setTitle(formatTexturedText(getPlayer(), this.texture, this.overlayTexture, this.text));
    }

    public static Text formatTexturedText(ServerPlayerEntity player, @Nullable Text texture, @Nullable Text overlayTexture, @Nullable Text input) {
        if (PolymerResourcePackUtils.hasMainPack(player)) {
            var text = Text.empty();
            var textTexture = Text.empty().setStyle(TEXTURE_STYLE);

            if (texture != null) {
                textTexture.append("c").append(texture).append("d");
            }

            if (overlayTexture != null) {
                textTexture.append("a").append(overlayTexture).append("b");
            }

            if (!textTexture.getSiblings().isEmpty()) {
                text.append(textTexture);
            }

            if (input != null) {
                text.append(input);
            }
            return text;
        } else {
            return input != null ? input : Text.empty();
        }
    }

    public static Text formatTexturedTextAnvil(ServerPlayerEntity player, @Nullable Text texture, @Nullable Text input) {
        if (PolymerResourcePackUtils.hasMainPack(player)) {
            var text = Text.empty();
            var textTexture = Text.empty().setStyle(TEXTURE_STYLE);

            if (texture != null) {
                textTexture.append("m").append(texture).append("n");
            }

            if (!textTexture.getSiblings().isEmpty()) {
                text.append(textTexture);
            }

            if (input != null) {
                text.append(input);
            }
            return text;
        } else {
            return input != null ? input : Text.empty();
        }
    }


    @Deprecated
    @Override
    public void setTitle(Text title) {
        super.setTitle(title);
    }

    public GuiElement filler() {
        return PolymerResourcePackUtils.hasMainPack(this.getPlayer()) ? GuiElement.EMPTY : GuiUtils.FILLER;
    }
}
