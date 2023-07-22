package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.api.v1.PolydexUtils;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.GuiInterface;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class IngredientGuiElement implements GuiElementInterface, GuiElementInterface.ClickCallback {
    protected final ItemStack[] items;
    protected int frame = 0;
    protected int tick = 0;
    private ItemStack currentItemStack = ItemStack.EMPTY;

    public IngredientGuiElement(ItemStack[] items) {
        this.items = items;
    }

    public ItemStack getItemStackForDisplay(GuiInterface gui) {
        int cFrame = this.frame;
        ++this.tick;
        if (this.tick >= 20) {
            this.tick = 0;
            ++this.frame;
            if (this.frame >= this.items.length) {
                this.frame = 0;
            }
        }

        return this.currentItemStack = this.items[cFrame].copy();
    }

    @Override
    public ClickCallback getGuiCallback() {
        return this;
    }

    @Override
    public ItemStack getItemStack() {
        return this.currentItemStack;
    }

    @Override
    public void click(int i, ClickType clickType, SlotActionType slotActionType, SlotGuiInterface slotGuiInterface) {
        boolean sound = false;
        if (clickType.isLeft) {
            sound = PolydexUtils.openRecipeListUi(slotGuiInterface.getPlayer(), this.currentItemStack, slotGuiInterface::open);
        } else if (clickType.isRight) {
            sound = PolydexUtils.openUsagesListUi(slotGuiInterface.getPlayer(), this.currentItemStack, slotGuiInterface::open);
        }

        if (sound) {
            GuiUtils.playClickSound(slotGuiInterface.getPlayer());
        }
    }
}
