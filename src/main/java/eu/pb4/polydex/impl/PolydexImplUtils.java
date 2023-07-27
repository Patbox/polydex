package eu.pb4.polydex.impl;

import eu.pb4.polydex.impl.book.ui.IngredientGuiElement;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

public class PolydexImplUtils {
    public static final Text DEFAULT_SEPARATOR = Text.literal(" | ").formatted(Formatting.DARK_GRAY);
    public static final Text SPACE_SEPARATOR = Text.literal(" ");

    public static Text mergeText(Collection<Text> texts, Text separator) {
        var out = Text.literal("");

        var iterator = texts.iterator();

        while (iterator.hasNext()) {
            out.append(iterator.next());

            if (iterator.hasNext()) {
                out.append(separator);
            }
        }

        return out;
    }

    public static Text mergeText(Collection<Text> texts) {
        var out = Text.literal("");

        var iterator = texts.iterator();

        if (iterator.hasNext()) {
            out.append(iterator.next());
        }

        return out;
    }

    public static GuiElementInterface getIngredientDisplay(Ingredient ingredient) {
        ItemStack[] stacks = PolydexImplUtils.readIngredient(ingredient);
        return getIngredientDisplay(stacks);
    }

    public static GuiElementInterface getIngredientDisplay(ItemStack[] stacks) {
        return stacks.length > 0 ? new IngredientGuiElement(stacks) : new GuiElement(ItemStack.EMPTY, GuiElement.EMPTY_CALLBACK);
    }

    public static ItemStack[] readIngredient(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getMatchingStacks();
        if (stacks.length > 0) {
            return stacks;
        } else {
            return new ItemStack[]{};
        }
    }
}
