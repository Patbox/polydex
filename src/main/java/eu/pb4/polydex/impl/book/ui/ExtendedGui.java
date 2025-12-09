package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.LayeredGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public class ExtendedGui extends LayeredGui {
    private static final Style INFO_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse("uniform")));
    public static final Style TEXTURE_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse("polydex:gui"))).withColor(ChatFormatting.WHITE);
    private Component text = Component.empty();
    private Component texture;
    private Component overlayTexture;
    private boolean lock = false;
    private boolean isDirty = false;

    public ExtendedGui(MenuType<?> type, ServerPlayer player, boolean manipulatePlayerSlots) {
        super(type, player, manipulatePlayerSlots);
    }
    
    public void setText(@Nullable Component text) {
        this.text = text;
        this.updateText();
    }
    
    
    public void setTexture(@Nullable Component text) {
        this.texture = text;
        this.updateText();
    }

    public void setOverlayTexture(@Nullable Component text) {
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

    public static Component formatTexturedText(ServerPlayer player, @Nullable Component texture, @Nullable Component overlayTexture, @Nullable Component input) {
        if (PolymerResourcePackUtils.hasMainPack(player)) {
            var text = Component.empty();
            var textTexture = Component.empty().setStyle(TEXTURE_STYLE);

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
            return input != null ? input : Component.empty();
        }
    }

    public static Component formatTexturedTextAnvil(ServerPlayer player, @Nullable Component texture, @Nullable Component input) {
        if (PolymerResourcePackUtils.hasMainPack(player)) {
            var text = Component.empty();
            var textTexture = Component.empty().setStyle(TEXTURE_STYLE);

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
            return input != null ? input : Component.empty();
        }
    }


    @Deprecated
    @Override
    public void setTitle(Component title) {
        super.setTitle(title);
    }

    public GuiElement filler() {
        return PolymerResourcePackUtils.hasMainPack(this.getPlayer()) ? GuiElement.EMPTY : GuiUtils.FILLER;
    }
}
