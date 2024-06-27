package eu.pb4.polydex.impl.book.view.crafting;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.TippedArrowRecipe;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TippedArrowRecipePage extends AbstractCraftingRecipePage<TippedArrowRecipe> {
    private static final Ingredient ARROW = Ingredient.ofItems(Items.ARROW);
    private static final List<PolydexIngredient<?>> INGREDIENTS = List.of(
            PolydexIngredient.of(ARROW),
            PolydexStack.of(Items.LINGERING_POTION)
    );

    public TippedArrowRecipePage(RecipeEntry<TippedArrowRecipe> recipe) {
        super(recipe);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.stack().getBacking() instanceof ItemStack stack && stack.isOf(Items.TIPPED_ARROW);
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return INGREDIENTS;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        var potion = getPotion(entry);
        var stack = new ItemStack(Items.TIPPED_ARROW);
        stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(potion));
        return stack;
    }

    @Override
    protected Ingredient getStacksAt(TippedArrowRecipe recipe, int x, int y, @Nullable PolydexEntry entry) {
        if (x == 1 && y == 1) {
            var potion = getPotion(entry);
            var stack = new ItemStack(Items.LINGERING_POTION);
            stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(potion));
            return Ingredient.ofStacks(stack);
        }
        return ARROW;
    }

    private RegistryEntry<Potion> getPotion(PolydexEntry entry) {
        if (entry != null && entry.stack().contains(DataComponentTypes.POTION_CONTENTS)) {
            return entry.stack().get(DataComponentTypes.POTION_CONTENTS).potion().orElse(Potions.WATER);
        }

        return Potions.WATER;
    }

    @Override
    protected ItemStack[] getOutput(TippedArrowRecipe recipe, ServerPlayerEntity player, @Nullable PolydexEntry entry) {
        var potion = getPotion(entry);
        var arrow = new ItemStack(Items.TIPPED_ARROW);
        arrow.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(potion));
        return new ItemStack[] { arrow };
    }
}
