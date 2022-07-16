package eu.pb4.polydex.api;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class PolydexUiElements {
    public static final GuiElement CRAFTING_RECIPE_ICON = new GuiElementBuilder(Items.CRAFTING_TABLE)
            .setName(Text.translatable("block.minecraft.crafting_table").append(" / ").append(Text.translatable("text.polydex.recipe.player_crafting")))
            .build();

    public static final GuiElement CRAFTING_TABLE_RECIPE_ICON = simple(Items.CRAFTING_TABLE);
    public static final GuiElement POTION_RECIPE_ICON = simple(Items.BREWING_STAND);
    public static final GuiElement SMITING_RECIPE_ICON = simple(Items.SMITHING_TABLE);
    public static final GuiElement STONECUTTING_RECIPE_ICON = simple(Items.STONECUTTER);

    public static final GuiElement INVALID_PAGE = new GuiElementBuilder(Items.STRUCTURE_VOID)
            .setName(Text.literal("[INVALID]").formatted(Formatting.RED))
            .build();


    private static GuiElement simple(Item item) {
        return new GuiElement(item.getDefaultStack(), GuiElementInterface.EMPTY_CALLBACK);
    }
}
