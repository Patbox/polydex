package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.GuiInterface;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class IngredientGuiElement implements GuiElementInterface, GuiElementInterface.ClickCallback {
    protected final PolydexStack<?>[] items;
    @Nullable
    private final Consumer<GuiElementBuilder> consumer;
    protected int frame = 0;
    protected int tick = 0;
    private ItemStack currentItemStack = ItemStack.EMPTY;

    public IngredientGuiElement(List<PolydexStack<?>> stacks, @Nullable Consumer<GuiElementBuilder> consumer) {
        this.items = stacks.toArray(new PolydexStack<?>[0]);
        this.consumer = consumer;
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
        var item = this.items[cFrame].toDisplayItemStack(gui.getPlayer());
        if (this.consumer != null) {
            var b = GuiElementBuilder.from(item);
            this.consumer.accept(b);
            item = b.asStack();
        }

        return this.currentItemStack = item;
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
            sound = PolydexPageUtils.openRecipeListUi(slotGuiInterface.getPlayer(), this.items[this.frame], slotGuiInterface::open);
        } else if (clickType.isRight) {
            sound = PolydexPageUtils.openUsagesListUi(slotGuiInterface.getPlayer(), this.items[this.frame], slotGuiInterface::open);
        }

        if (sound) {
            GuiUtils.playClickSound(slotGuiInterface.getPlayer());
        }
    }
}
