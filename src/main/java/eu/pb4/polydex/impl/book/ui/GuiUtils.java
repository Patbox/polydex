package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.Items;

public class GuiUtils {


    public static final GuiElement EMPTY = GuiElement.EMPTY;
    public static final GuiElement EMPTY_STACK = new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .model(requestModel("empty"))
            .hideTooltip().build();
    public static final GuiElement FILLER = new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                .setName(Component.empty())
                .hideTooltip().build();
    private static final Identifier BACK_TEXTURE = requestModel("back");
    private static final Identifier NEXT_PAGE_TEXTURE = requestModel("next_page");
    private static final Identifier PREVIOUS_PAGE_TEXTURE = requestModel("previous_page");
    private static final Identifier FLAME_TEXTURE = requestModel("flame");
    private static final Identifier XP_TEXTURE = requestModel("xp");
    private static final Identifier PAGE_TEXTURE = requestModel("page");

    private static Identifier requestModel(String back) {
        return ResourcePackExtras.bridgeModel(PolydexImpl.id("sgui/elements/" + back));
    }

    public static void register() {
    }

    public static GuiElementBuilder page(ServerPlayer player, int current, int max) {
        return (new GuiElementBuilder(Items.BOOK)).noDefaults().setName(
                Component.translatable("text.polydex.view.pages",
                        Component.literal("" + current).withStyle(ChatFormatting.WHITE),
                        Component.literal("" + max).withStyle(ChatFormatting.WHITE)
                ).withStyle(ChatFormatting.AQUA)
        );
    }

    public static GuiElementBuilder flame(ServerPlayer player) {
        return hasTexture(player) ?
                new GuiElementBuilder(FLAME_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.BLAZE_POWDER).noDefaults();
    }

    public static GuiElementBuilder xp(ServerPlayer player) {
        return hasTexture(player) ?
                new GuiElementBuilder(XP_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.EXPERIENCE_BOTTLE).noDefaults();
    }

    public static GuiElement backButton(ServerPlayer player, Runnable callback, boolean back) {
        return backBase(player)
                .setName(Component.translatable(back ? "gui.back" : "test.polydex.close").withStyle(ChatFormatting.RED))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    callback.run();
                }).build();
    }

    private static GuiElementBuilder backBase(ServerPlayer player) {
        return hasTexture(player) ?
                new GuiElementBuilder(BACK_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.STRUCTURE_VOID).noDefaults();
    }

    public static final void playClickSound(ServerPlayer player) {
        player.connection.send(new ClientboundSoundEntityPacket(
                SoundEvents.UI_BUTTON_CLICK, SoundSource.UI, player, 0.5f, 1, player.getRandom().nextLong()
        ));
    }

    public static GuiElement nextPage(ServerPlayer player, PageAware gui) {
        return nextPageBase(player)
                .setName(Component.translatable("spectatorMenu.next_page").withStyle(ChatFormatting.WHITE))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.nextPage();
                }).build();
    }

    private static GuiElementBuilder nextPageBase(ServerPlayer player) {
        return hasTexture(player)
                ? new GuiElementBuilder(NEXT_PAGE_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.PLAYER_HEAD).noDefaults().setSkullOwner(GuiHeadTextures.GUI_NEXT_PAGE);
    }

    private static GuiElementBuilder previousPageBase(ServerPlayer player) {
        return hasTexture(player)
                ? new GuiElementBuilder(PREVIOUS_PAGE_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.PLAYER_HEAD).noDefaults().setSkullOwner(GuiHeadTextures.GUI_PREVIOUS_PAGE);
    }

    public static GuiElement previousPage(ServerPlayer player, PageAware gui) {
        return previousPageBase(player)
                .setName(Component.translatable("spectatorMenu.previous_page").withStyle(ChatFormatting.WHITE))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.previousPage();
                }).build();
    }

    public static boolean hasTexture(ServerPlayer player) {
        return PolymerResourcePackUtils.hasMainPack(player);
    }

    public static GuiElementInterface fillerStack(ServerPlayer player) {
        return hasTexture(player) ? EMPTY_STACK : FILLER;
    }
}
