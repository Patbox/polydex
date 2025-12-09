package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.impl.PolydexImplUtils;
import eu.pb4.polydex.mixin.SmithingTrimRecipeAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class SmithingTrimRecipePage extends AbstractSmithingRecipeView<SmithingTrimRecipe> {
    public SmithingTrimRecipePage(RecipeHolder<SmithingTrimRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected Ingredient getTemplate() {
        return recipe.templateIngredient().orElse(null);
    }

    @Override
    protected Ingredient getAddition() {
        return recipe.additionIngredient().orElse(null);
    }

    @Override
    protected Ingredient getBase() {
        return recipe.baseIngredient();
    }

    @Override
    protected SlotDisplay getBaseItem(@Nullable PolydexEntry entry) {
        return entry != null && getBase().test((ItemStack) entry.stack().getBacking()) ? new SlotDisplay.ItemStackSlotDisplay((ItemStack) entry.stack().getBacking()) : new SlotDisplay.ItemSlotDisplay(Items.IRON_CHESTPLATE);
    }

    @Override
    public String getGroup() {
        return "trimming";
    }

    @Override
    public boolean syncWithClient(ServerPlayer player) {
        return false;
    }

    @Override
    public int priority() {
        return -100;
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        var list = new ArrayList<ItemStack>();
        var trim = ((SmithingTrimRecipeAccessor) this.recipe).getPattern();

        var baseStack = entry != null && getBase().test((ItemStack) entry.stack().getBacking()) ? (ItemStack) entry.stack().getBacking() : Items.IRON_CHESTPLATE.getDefaultInstance();
        for (var material : PolydexImplUtils.readIngredient(getAddition())) {
            var optional = TrimMaterials.getFromIngredient(server.registryAccess(), material);
            if (optional.isPresent()) {
                ItemStack itemStack2 = baseStack.copy();
                itemStack2.setCount(1);
                itemStack2.set(DataComponents.TRIM, new ArmorTrim(optional.get(), trim));
                list.add(itemStack2);
            }
        }

        return list.getFirst();
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.stack().getBackingClass() == ItemStack.class && getBase().test((ItemStack) entry.stack().getBacking());
    }
}
