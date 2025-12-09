package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TippedArrowRecipePage extends AbstractCraftingRecipePage<TippedArrowRecipe> {
    private static final Ingredient ARROW = Ingredient.of(Items.ARROW);

    public TippedArrowRecipePage(RecipeHolder<TippedArrowRecipe> recipe) {
        super(recipe);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.stack().getBacking() instanceof ItemStack stack && stack.is(Items.TIPPED_ARROW);
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        var potion = getPotion(entry);
        var stack = new ItemStack(Items.TIPPED_ARROW);
        stack.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withPotion(potion));
        return stack;
    }

    @Override
    protected SlotDisplay getStacksAt(TippedArrowRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        if (x == 1 && y == 1) {
            var potion = getPotion(entry);
            var stack = new ItemStack(Items.LINGERING_POTION);
            stack.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withPotion(potion));
            return new SlotDisplay.ItemStackSlotDisplay(stack);
        }
        return new SlotDisplay.ItemSlotDisplay(Items.ARROW);
    }

    private Holder<Potion> getPotion(PolydexEntry entry) {
        if (entry != null && entry.stack().contains(DataComponents.POTION_CONTENTS)) {
            return entry.stack().get(DataComponents.POTION_CONTENTS).potion().orElse(Potions.WATER);
        }

        return Potions.WATER;
    }

    @Override
    public ItemStack getOutput(PolydexEntry entry, MinecraftServer server) {
        var potion = getPotion(entry);
        var arrow = new ItemStack(Items.TIPPED_ARROW);
        arrow.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withPotion(potion));
        arrow.setCount(8);
        return arrow;
    }
}
