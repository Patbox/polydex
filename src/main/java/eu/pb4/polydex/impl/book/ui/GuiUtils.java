package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class GuiUtils {


    public static final GuiElement EMPTY = GuiElement.EMPTY;
    public static final GuiElement FILLER = Util.make(() -> new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                .setName(Text.empty())
                .hideTooltip().build());
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

    public static GuiElementBuilder page(ServerPlayerEntity player, int current, int max) {
        return (new GuiElementBuilder(Items.BOOK)).noDefaults().setName(
                Text.translatable("text.polydex.view.pages",
                        Text.literal("" + current).formatted(Formatting.WHITE),
                        Text.literal("" + max).formatted(Formatting.WHITE)
                ).formatted(Formatting.AQUA)
        );
    }

    public static GuiElementBuilder flame(ServerPlayerEntity player) {
        return hasTexture(player) ?
                new GuiElementBuilder(FLAME_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.BLAZE_POWDER).noDefaults();
    }

    public static GuiElementBuilder xp(ServerPlayerEntity player) {
        return hasTexture(player) ?
                new GuiElementBuilder(XP_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.EXPERIENCE_BOTTLE).noDefaults();
    }

    public static GuiElement backButton(ServerPlayerEntity player, Runnable callback, boolean back) {
        return backBase(player)
                .setName(Text.translatable(back ? "gui.back" : "test.polydex.close").formatted(Formatting.RED))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    callback.run();
                }).build();
    }

    private static GuiElementBuilder backBase(ServerPlayerEntity player) {
        return hasTexture(player) ?
                new GuiElementBuilder(BACK_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.STRUCTURE_VOID).noDefaults();
    }

    public static final void playClickSound(ServerPlayerEntity player) {
        player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
    }

    public static GuiElement nextPage(ServerPlayerEntity player, PageAware gui) {
        return nextPageBase(player)
                .setName(Text.translatable("spectatorMenu.next_page").formatted(Formatting.WHITE))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.nextPage();
                }).build();
    }

    private static GuiElementBuilder nextPageBase(ServerPlayerEntity player) {
        return hasTexture(player)
                ? new GuiElementBuilder(NEXT_PAGE_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.PLAYER_HEAD).noDefaults().setSkullOwner(GuiHeadTextures.GUI_NEXT_PAGE);
    }

    private static GuiElementBuilder previousPageBase(ServerPlayerEntity player) {
        return hasTexture(player)
                ? new GuiElementBuilder(PREVIOUS_PAGE_TEXTURE).noDefaults()
                : new GuiElementBuilder(Items.PLAYER_HEAD).noDefaults().setSkullOwner(GuiHeadTextures.GUI_PREVIOUS_PAGE);
    }

    public static GuiElement previousPage(ServerPlayerEntity player, PageAware gui) {
        return previousPageBase(player)
                .setName(Text.translatable("spectatorMenu.previous_page").formatted(Formatting.WHITE))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.previousPage();
                }).build();
    }

    private static boolean hasTexture(ServerPlayerEntity player) {
        return PolymerResourcePackUtils.hasMainPack(player);
    }
}
