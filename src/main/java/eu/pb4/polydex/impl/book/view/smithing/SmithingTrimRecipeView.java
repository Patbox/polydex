package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.recipe.ItemEntry;
import eu.pb4.polydex.impl.PolydexImplUtils;
import eu.pb4.polydex.mixin.SmithingTrimRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimMaterials;
import net.minecraft.item.trim.ArmorTrimPatterns;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SmithingTrimRecipe;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Optional;

public class SmithingTrimRecipeView extends AbstractSmithingRecipeView<SmithingTrimRecipe> {
    private static final Ingredient DEFAULT = Ingredient.ofItems(Items.IRON_CHESTPLATE);

    @Override
    protected Ingredient getTemplate(SmithingTrimRecipe recipe) {
        return cast(recipe).getTemplate();
    }

    @Override
    protected Ingredient getAddition(SmithingTrimRecipe recipe) {
        return cast(recipe).getAddition();
    }

    @Override
    protected Ingredient getBase(SmithingTrimRecipe recipe) {
        return cast(recipe).getBase();
    }

    @Override
    protected Ingredient getBaseItem(ItemEntry entry, SmithingTrimRecipe recipe) {
        return getBase(recipe).test(entry.stack()) ? Ingredient.ofStacks(entry.stack()) : DEFAULT;
    }

    @Override
    public int priority(SmithingTrimRecipe recipe) {
        return -100;
    }

    @Override
    protected ItemStack[] getOutput(ItemEntry entry, ServerPlayerEntity player, SmithingTrimRecipe recipe) {
        var list = new ArrayList<ItemStack>();
        var trim = getTemplate(recipe).getMatchingStacks()[0];
        var optional2 = ArmorTrimPatterns.get(player.server.getRegistryManager(), trim);

        var baseStack = getBase(recipe).test(entry.stack()) ? entry.stack() : Items.IRON_CHESTPLATE.getDefaultStack();

        for (var material : PolydexImplUtils.readIngredient(getAddition(recipe))) {
            Optional<RegistryEntry.Reference<ArmorTrimMaterial>> optional = ArmorTrimMaterials.get(player.server.getRegistryManager(), material);
            if (optional.isPresent() && optional2.isPresent()) {
                Optional<ArmorTrim> optional3 = ArmorTrim.getTrim(player.server.getRegistryManager(), trim);
                if (optional3.isPresent() && optional3.get().equals(optional2.get(), optional.get())) {
                    continue;
                }

                ItemStack itemStack2 = baseStack.copy();
                itemStack2.setCount(1);
                ArmorTrim armorTrim = new ArmorTrim(optional.get(), optional2.get());
                if (ArmorTrim.apply(player.server.getRegistryManager(), itemStack2, armorTrim)) {
                    list.add(itemStack2);
                }
            }
        }

        return list.toArray(new ItemStack[0]);
    }

    @Override
    public boolean isOwner(MinecraftServer server, ItemEntry entry, SmithingTrimRecipe object) {
        return getBase(object).test(entry.stack());
    }

    private SmithingTrimRecipeAccessor cast(SmithingTrimRecipe recipe) {
        return (SmithingTrimRecipeAccessor) recipe;
    }
}
