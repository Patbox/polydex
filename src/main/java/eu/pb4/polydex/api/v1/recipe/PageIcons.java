package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PageIcons {
    public static final ItemStack CRAFTING_RECIPE_ICON = new GuiElementBuilder(Items.CRAFTING_TABLE)
            .setName(Component.translatable("block.minecraft.crafting_table").append(" / ").append(Component.translatable("text.polydex.recipe.player_crafting")))
            .asStack();

    public static final ItemStack CRAFTING_TABLE_RECIPE_ICON = Items.CRAFTING_TABLE.getDefaultInstance();
    public static final ItemStack POTION_RECIPE_ICON = Items.BREWING_STAND.getDefaultInstance();
    public static final ItemStack SMITING_RECIPE_ICON = Items.SMITHING_TABLE.getDefaultInstance();
    public static final ItemStack STONECUTTING_RECIPE_ICON = Items.STONECUTTER.getDefaultInstance();
    public static final ItemStack AXE_ICON = Items.IRON_AXE.getDefaultInstance();

    public static final ItemStack INVALID_PAGE = new GuiElementBuilder(Items.STRUCTURE_VOID)
            .setName(Component.literal("[INVALID]").withStyle(ChatFormatting.RED))
            .asStack();

}
