package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.api.recipe.PageBuilder;
import eu.pb4.polydex.impl.PolydexImplUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class LayerBuilder extends Layer implements PageBuilder {
    private final ItemEntry currentEntry;

    public LayerBuilder(ServerPlayerEntity player, ItemEntry currentEntry) {
        super(5, 9);
        this.currentEntry = currentEntry;
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
    public void setIngredient(int x, int y, ItemStack... stacks) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(stacks));
    }

    @Override
    public void setIngredient(int x, int y, Ingredient ingredient) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(ingredient));
    }

    @Override
    public void setEmpty(int x, int y) {
        this.setSlot(index(x, y), ItemStack.EMPTY);
    }

    public void clear() {
        var fill = new GuiElementBuilder(Items.BLACK_STAINED_GLASS_PANE).setName(Text.empty()).build();
        for (int i = 0, size = this.size; i < size; i++) {
            this.setSlot(i, fill);
        }
    }
}
