package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.impl.PolydexImplUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimMaterials;
import net.minecraft.item.equipment.trim.ArmorTrimPatterns;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.SmithingTrimRecipe;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class SmithingTrimRecipePage extends AbstractSmithingRecipeView<SmithingTrimRecipe> {
    public SmithingTrimRecipePage(RecipeEntry<SmithingTrimRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected Ingredient getTemplate() {
        return recipe.template().orElse(null);
    }

    @Override
    protected Ingredient getAddition() {
        return recipe.addition().orElse(null);
    }

    @Override
    protected Ingredient getBase() {
        return recipe.base().orElse(null);
    }

    @Override
    protected SlotDisplay getBaseItem(@Nullable PolydexEntry entry) {
        return entry != null && getBase().test((ItemStack) entry.stack().getBacking()) ? new SlotDisplay.StackSlotDisplay((ItemStack) entry.stack().getBacking()) : new SlotDisplay.ItemSlotDisplay(Items.IRON_CHESTPLATE);
    }

    @Override
    public String getGroup() {
        return "trimming";
    }

    @Override
    public boolean syncWithClient(ServerPlayerEntity player) {
        return false;
    }

    @Override
    public int priority() {
        return -100;
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        var list = new ArrayList<ItemStack>();
        var trim = getTemplate().getMatchingItems().findFirst().get();
        var optional2 = ArmorTrimPatterns.get(server.getRegistryManager(), trim.value().getDefaultStack());

        var baseStack = entry != null && getBase().test((ItemStack) entry.stack().getBacking()) ? (ItemStack) entry.stack().getBacking() : Items.IRON_CHESTPLATE.getDefaultStack();
        for (var material : PolydexImplUtils.readIngredient(getAddition())) {
            Optional<RegistryEntry.Reference<ArmorTrimMaterial>> optional = ArmorTrimMaterials.get(server.getRegistryManager(), material);
            if (optional.isPresent() && optional2.isPresent()) {
                //Optional<ArmorTrim> optional3 = ArmorTrim.getTrim(player.server.getRegistryManager(), trim, true);
                //if (optional3.isPresent() && optional3.get().equals(optional2.get(), optional.get())) {
                //    continue;
                //}

                ItemStack itemStack2 = baseStack.copy();
                itemStack2.setCount(1);
                itemStack2.set(DataComponentTypes.TRIM, new ArmorTrim(optional.get(), optional2.get()));
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
