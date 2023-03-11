package eu.pb4.polydex.api;

import eu.pb4.polydex.impl.IngredientGuiElement;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.EntryViewerGui;
import eu.pb4.polydex.impl.book.GuiUtils;
import eu.pb4.polydex.impl.book.MainIndexGui;
import eu.pb4.sgui.api.elements.AnimatedGuiElement;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class PolydexUtils {
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

    public static Text createText(ItemStack stack) {
        if (stack.isEmpty()) {
            return Text.translatable("text.polydex.empty");
        } else if (stack.getCount() == 1) {
            return stack.getName();
        } else {
            return Text.literal("" + stack.getCount() + " × ").append(stack.getName());
        }
    }

    @Nullable
    public static ItemEntry getItemEntryFor(ItemStack stack) {
        return PolydexImpl.getEntry(stack);
    }

    public static boolean openRecipeListUi(ServerPlayerEntity player, ItemStack stack, Runnable closeCallback) {
        var entry = getItemEntryFor(stack);
        if (entry == null) {
            return false;
        }

        if (entry.getVisiblePagesSize(player) > 0) {
            new EntryViewerGui(player, entry, false, closeCallback).open();
            return true;
        }

        return false;
    }

    public static boolean openUsagesListUi(ServerPlayerEntity player, ItemStack stack, Runnable closeCallback) {
        var entry = getItemEntryFor(stack);
        if (entry == null) {
            return false;
        }

        if (entry.getVisibleIngredientPagesSize(player) > 0) {
            new EntryViewerGui(player, entry, true, closeCallback).open();
            return true;
        }

        return false;
    }

    public static GuiElementInterface getIngredientDisplay(Ingredient ingredient) {
        ItemStack[] stacks = PolydexUtils.readIngredient(ingredient);
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
