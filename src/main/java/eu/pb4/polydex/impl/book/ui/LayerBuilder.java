package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polydex.impl.PolydexImplUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class LayerBuilder extends Layer implements PageBuilder {
    private final ServerPlayerEntity player;
    public Text texture;

    public LayerBuilder(ServerPlayerEntity player) {
        super(5, 9);
        this.player = player;
    }

    @Override
    public void set(int x, int y, ItemStack stack) {
        this.setSlot(index(x, y), stack);
    }

    private int index(int x, int y) {
        return x + y * 9;
    }

    @Override
    public void setOutput(int x, int y, ItemStack... stack) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(stack));
    }

    @Override
    public void setOutput(int x, int y, PolydexStack<?>... stacks) {
        var list = new ArrayList<ItemStack>(stacks.length);

        for (var stack : stacks) {
            list.add(stack.toItemStack(this.player));
        }

        setOutput(x, y, list.toArray(new ItemStack[0]));
    }

    @Override
    public void setIngredient(int x, int y, ItemStack... stacks) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(stacks));
    }

    @Override
    public void setIngredient(int x, int y, Ingredient ingredient) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(ingredient));
    }

    @Override
    public void setIngredient(int x, int y, PolydexIngredient<?> ingredient) {
        var stacks = ingredient.asStacks();
        var list = new ArrayList<ItemStack>(stacks.size());

        for (var stack : stacks) {
            list.add(stack.toItemStack(this.player));
        }

        setOutput(x, y, list.toArray(new ItemStack[0]));
    }

    @Override
    public void setEmpty(int x, int y) {
        this.setSlot(index(x, y), ItemStack.EMPTY);
    }

    @Override
    public boolean hasTextures() {
        return PolymerResourcePackUtils.hasPack(this.player);
    }

    public void clear(GuiElement filler) {
        for (int i = 0, size = this.size; i < size; i++) {
            this.setSlot(i, filler);
        }
    }
}
