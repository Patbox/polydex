package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class PageIcons {
    public static final ItemStack CRAFTING_RECIPE_ICON = new GuiElementBuilder(Items.CRAFTING_TABLE)
            .setName(Text.translatable("block.minecraft.crafting_table").append(" / ").append(Text.translatable("text.polydex.recipe.player_crafting")))
            .asStack();

    public static final ItemStack CRAFTING_TABLE_RECIPE_ICON = Items.CRAFTING_TABLE.getDefaultStack();
    public static final ItemStack POTION_RECIPE_ICON = Items.BREWING_STAND.getDefaultStack();
    public static final ItemStack SMITING_RECIPE_ICON = Items.SMITHING_TABLE.getDefaultStack();
    public static final ItemStack STONECUTTING_RECIPE_ICON = Items.STONECUTTER.getDefaultStack();
    public static final ItemStack AXE_ICON = Items.IRON_AXE.getDefaultStack();

    public static final ItemStack INVALID_PAGE = new GuiElementBuilder(Items.STRUCTURE_VOID)
            .setName(Text.literal("[INVALID]").formatted(Formatting.RED))
            .asStack();

}
