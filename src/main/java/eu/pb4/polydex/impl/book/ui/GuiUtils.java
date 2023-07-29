package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public class GuiUtils {


    public static final GuiElement EMPTY = GuiElement.EMPTY;
    public static final GuiElement FILLER = Util.make(() -> {
        var b = new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                .setName(Text.empty())
                .hideFlags();
        b.getOrCreateNbt().putBoolean("polydex:filler", true);
        return b.build();
    });
    private static final PolymerModelData BACK_TEXTURE = requestModel(Items.STRUCTURE_VOID, "back");
    private static final PolymerModelData NEXT_PAGE_TEXTURE = requestModel(Items.STRUCTURE_VOID, "next_page");
    private static final PolymerModelData PREVIOUS_PAGE_TEXTURE = requestModel(Items.STRUCTURE_VOID, "previous_page");
    private static final PolymerModelData FLAME_TEXTURE = requestModel(Items.STRUCTURE_VOID, "flame");
    private static final PolymerModelData XP_TEXTURE = requestModel(Items.STRUCTURE_VOID, "xp");
    private static final PolymerModelData PAGE_TEXTURE = requestModel(Items.STRUCTURE_VOID, "page");

    private static PolymerModelData requestModel(Item item, String back) {
        return PolymerResourcePackUtils.requestModel(item, PolydexImpl.id("sgui/elements/" + back));
    }

    public static void register() {
    }

    public static GuiElementBuilder page(ServerPlayerEntity player, int current, int max) {
        return (new GuiElementBuilder(Items.BOOK)).setName(
                Text.translatable("text.polydex.view.pages",
                        Text.literal("" + current).formatted(Formatting.WHITE),
                        Text.literal("" + max).formatted(Formatting.WHITE)
                ).formatted(Formatting.AQUA)
        );
    }

    public static GuiElementBuilder flame(ServerPlayerEntity player) {
        return hasTexture(player) ?
                new GuiElementBuilder(FLAME_TEXTURE.item()).setCustomModelData(FLAME_TEXTURE.value())
                : new GuiElementBuilder(Items.BLAZE_POWDER);
    }

    public static GuiElementBuilder xp(ServerPlayerEntity player) {
        return hasTexture(player) ?
                new GuiElementBuilder(XP_TEXTURE.item()).setCustomModelData(XP_TEXTURE.value())
                : new GuiElementBuilder(Items.EXPERIENCE_BOTTLE);
    }

    public static GuiElement backButton(ServerPlayerEntity player, Runnable callback, boolean back) {
        return backBase(player)
                .setName(Text.translatable(back ? "gui.back" : "test.polydex.close").formatted(Formatting.RED))
                .hideFlags()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    callback.run();
                }).build();
    }

    private static GuiElementBuilder backBase(ServerPlayerEntity player) {
        return hasTexture(player) ?
                new GuiElementBuilder(BACK_TEXTURE.item()).setCustomModelData(BACK_TEXTURE.value())
                : new GuiElementBuilder(Items.STRUCTURE_VOID);
    }

    public static final void playClickSound(ServerPlayerEntity player) {
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
    }

    public static GuiElement nextPage(ServerPlayerEntity player, PageAware gui) {
        return nextPageBase(player)
                .setName(Text.translatable("spectatorMenu.next_page").formatted(Formatting.WHITE))
                .hideFlags()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.nextPage();
                }).build();
    }

    private static GuiElementBuilder nextPageBase(ServerPlayerEntity player) {
        return hasTexture(player)
                ? new GuiElementBuilder(NEXT_PAGE_TEXTURE.item()).setCustomModelData(NEXT_PAGE_TEXTURE.value())
                : new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(GuiHeadTextures.GUI_NEXT_PAGE);
    }

    private static GuiElementBuilder previousPageBase(ServerPlayerEntity player) {
        return hasTexture(player)
                ? new GuiElementBuilder(PREVIOUS_PAGE_TEXTURE.item()).setCustomModelData(PREVIOUS_PAGE_TEXTURE.value())
                : new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(GuiHeadTextures.GUI_PREVIOUS_PAGE);
    }

    public static GuiElement previousPage(ServerPlayerEntity player, PageAware gui) {
        return previousPageBase(player)
                .setName(Text.translatable("spectatorMenu.previous_page").formatted(Formatting.WHITE))
                .hideFlags()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.previousPage();
                }).build();
    }

    private static boolean hasTexture(ServerPlayerEntity player) {
        return PolymerResourcePackUtils.hasPack(player);
    }
}
