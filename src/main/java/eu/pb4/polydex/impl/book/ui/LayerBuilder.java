package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polydex.impl.PolydexImplUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.AnimatedGuiElement;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class LayerBuilder extends Layer implements PageBuilder {
    private final ServerPlayerEntity player;
    private final ContextParameterMap context;

    public LayerBuilder(ServerPlayerEntity player) {
        super(5, 9);
        this.player = player;
        this.context = new ContextParameterMap.Builder()
                .add(SlotDisplayContexts.FUEL_REGISTRY, player.getWorld().getFuelRegistry())
                .add(SlotDisplayContexts.REGISTRIES, player.getWorld().getRegistryManager())
                .build(SlotDisplayContexts.CONTEXT_TYPE);
    }

    @Override
    public void set(int x, int y, ItemStack stack) {
        this.setSlot(index(x, y), stack);
    }

    @Override
    public void set(int x, int y, ItemStack... stack) {
        this.setSlot(index(x, y), new AnimatedGuiElement(stack, 20, false, GuiElementInterface.EMPTY_CALLBACK));
    }

    @Override
    public void set(int x, int y, SlotDisplay display) {
        this.set(x, y, display.getStacks(context).toArray(ItemStack[]::new));
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
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(List.of(stacks), null));
    }

    @Override
    public void setOutput(int x, int y, SlotDisplay display) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(display.getStacks(context)));
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
    public void setIngredient(int x, int y, Optional<Ingredient> ingredient) {
        ingredient.ifPresent(value -> setIngredient(x, y, value));
    }

    @Override
    public void setIngredient(int x, int y, SlotDisplay display) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(display.getStacks(context)));
    }

    @Override
    public void setIngredient(int x, int y, PolydexIngredient<?> ingredient) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(ingredient, null));
    }

    @Override
    public void setIngredient(int x, int y, PolydexIngredient<?> ingredient, Consumer<GuiElementBuilder> builderConsumer) {
        this.setSlot(index(x, y), PolydexImplUtils.getIngredientDisplay(ingredient, builderConsumer));
    }

    @Override
    public void setEmpty(int x, int y) {
        this.setSlot(index(x, y), ItemStack.EMPTY);
    }

    @Override
    public boolean hasTextures() {
        return PolymerResourcePackUtils.hasMainPack(this.player);
    }

    public void clear(GuiElement filler) {
        for (int i = 0, size = this.size; i < size; i++) {
            this.setSlot(i, filler);
        }
    }
}
