package eu.pb4.polydex.impl.book.view.smithing;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.impl.PolydexImplUtils;
import eu.pb4.polydex.mixin.SmithingTrimRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimMaterials;
import net.minecraft.item.trim.ArmorTrimPatterns;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.SmithingTrimRecipe;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class SmithingTrimRecipePage extends AbstractSmithingRecipeView<SmithingTrimRecipe> {
    private static final Ingredient DEFAULT = Ingredient.ofItems(Items.IRON_CHESTPLATE);

    public SmithingTrimRecipePage(RecipeEntry<SmithingTrimRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected Ingredient getTemplate() {
        return cast(recipe).getTemplate();
    }

    @Override
    protected Ingredient getAddition() {
        return cast(recipe).getAddition();
    }

    @Override
    protected Ingredient getBase() {
        return cast(recipe).getBase();
    }

    @Override
    protected Ingredient getBaseItem(@Nullable PolydexEntry entry) {
        return entry != null && getBase().test((ItemStack) entry.stack().getBacking()) ? Ingredient.ofStacks((ItemStack) entry.stack().getBacking()) : DEFAULT;
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
    protected ItemStack[] getOutput(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        var list = new ArrayList<ItemStack>();
        var trim = getTemplate().getMatchingStacks()[0];
        var optional2 = ArmorTrimPatterns.get(player.server.getRegistryManager(), trim);

        var baseStack = entry != null && getBase().test((ItemStack) entry.stack().getBacking()) ? (ItemStack) entry.stack().getBacking() : Items.IRON_CHESTPLATE.getDefaultStack();

        for (var material : PolydexImplUtils.readIngredient(getAddition())) {
            Optional<RegistryEntry.Reference<ArmorTrimMaterial>> optional = ArmorTrimMaterials.get(player.server.getRegistryManager(), material);
            if (optional.isPresent() && optional2.isPresent()) {
                Optional<ArmorTrim> optional3 = ArmorTrim.getTrim(player.server.getRegistryManager(), trim, true);
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
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.stack().getBackingClass() == ItemStack.class && getBase().test((ItemStack) entry.stack().getBacking());
    }

    private SmithingTrimRecipeAccessor cast(SmithingTrimRecipe recipe) {
        return (SmithingTrimRecipeAccessor) recipe;
    }
}
