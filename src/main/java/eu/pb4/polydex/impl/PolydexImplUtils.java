package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polydex.impl.book.ui.IngredientGuiElement;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class PolydexImplUtils {
    public static final Component DEFAULT_SEPARATOR = Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY);
    public static final Component SPACE_SEPARATOR = Component.literal(" ");

    public static Component mergeText(Collection<Component> texts, Component separator) {
        var out = Component.empty();

        var iterator = texts.iterator();

        while (iterator.hasNext()) {
            out.append(iterator.next());

            if (iterator.hasNext()) {
                out.append(separator);
            }
        }

        return out;
    }

    public static Component mergeText(Collection<Component> texts) {
        var out = Component.empty();

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
        var stacks = PolydexImplUtils.readIngredient(ingredient);
        return getIngredientDisplay(stacks);
    }

    public static GuiElementInterface getIngredientDisplay(ItemStack[] stacks) {
        return getIngredientDisplay(List.of(stacks));
    }

    public static GuiElementInterface getIngredientDisplay(Collection<ItemStack> stacks) {
        var list = new ArrayList<PolydexStack<?>>(stacks.size());
        for (var stack : stacks) {
            list.add(PolydexStack.of(stack));
        }

        return getIngredientDisplay(list, null);
    }

    public static GuiElementInterface getIngredientDisplay(PolydexIngredient<?> ingredient, Consumer<GuiElementBuilder> consumer) {
        //noinspection unchecked
        return getIngredientDisplay((List<PolydexStack<?>>) (Object) ingredient.asStacks(), consumer);
    }

    public static List<ItemStack> readIngredient(@Nullable Ingredient ingredient) {
        if (ingredient == null) {
            return List.of();
        }
        return ingredient.items().map(ItemStack::new).toList();
    }


}
