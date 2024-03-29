package eu.pb4.polydex.api.v1.recipe;

import eu.pb4.sgui.api.elements.AnimatedGuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

public interface PageBuilder {
    void set(int x, int y, ItemStack stack);
    void set(int x, int y, ItemStack... stack);
    default void set(int x, int y, GuiElementBuilder builder) {
        set(x, y, builder.asStack());
    }
    void setOutput(int x, int y, ItemStack... stack);
    void setOutput(int x, int y, PolydexStack<?>... stack);
    void setIngredient(int x, int y, ItemStack... stacks);
    void setIngredient(int x, int y, Ingredient ingredient);
    void setIngredient(int x, int y, PolydexIngredient<?> ingredient);
    void setEmpty(int x, int y);

    default int width() {
        return 9;
    }

    default int height() {
        return 5;
    }

    boolean hasTextures();
}
