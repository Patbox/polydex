package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polydex.impl.book.ui.IngredientGuiElement;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class PolydexImplUtils {
    public static final Text DEFAULT_SEPARATOR = Text.literal(" | ").formatted(Formatting.DARK_GRAY);
    public static final Text SPACE_SEPARATOR = Text.literal(" ");

    public static Text mergeText(Collection<Text> texts, Text separator) {
        var out = Text.empty();

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
        var out = Text.empty();

        var iterator = texts.iterator();

        if (iterator.hasNext()) {
            out.append(iterator.next());
        }

        return out;
    }

    public static GuiElementInterface getIngredientDisplay(List<PolydexStack<?>> stacks, @Nullable Consumer<GuiElementBuilder> consumer) {
        return stacks.isEmpty() ? new GuiElement(ItemStack.EMPTY, GuiElement.EMPTY_CALLBACK) : new IngredientGuiElement(stacks, consumer);
    }

    public static GuiElementInterface getIngredientDisplay(Ingredient ingredient) {
        ItemStack[] stacks = PolydexImplUtils.readIngredient(ingredient);
        return getIngredientDisplay(stacks);
    }

    public static GuiElementInterface getIngredientDisplay(ItemStack[] stacks) {
        var list = new ArrayList<PolydexStack<?>>(stacks.length);
        for (var stack : stacks) {
            list.add(PolydexStack.of(stack));
        }

        return getIngredientDisplay(list, null);
    }

    public static GuiElementInterface getIngredientDisplay(PolydexIngredient<?> ingredient, Consumer<GuiElementBuilder> consumer) {
        //noinspection unchecked
        return getIngredientDisplay((List<PolydexStack<?>>) (Object) ingredient.asStacks(), consumer);
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
