package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public class BrewingRecipePage<T> extends AbstractRecipePolydexPage<BrewingRecipe> {
    public BrewingRecipePage(RecipeHolder<BrewingRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected List<PolydexIngredient<?>> getIngredientsFromRecipe(RecipeHolder<BrewingRecipe> recipe) {
        return List.of(PolydexIngredient.of(recipe.value().getInput()), PolydexIngredient.of(recipe.value().getReagent()));
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry entry, MinecraftServer server) {
        return this.recipe.getOutput().create();
    }

    @Override
    public boolean syncWithClient(ServerPlayer player) {
        return false;
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return InternalPageTextures.POTION;
    }

    @Override
    public List<PolydexCategory> categories() {
        return List.of(PolydexCategory.BREWING);
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return PageIcons.POTION_RECIPE_ICON;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder builder) {
        var out = this.recipe.getOutput().create();

        builder.setIngredient(3, 1, stackFromPotionIngredient(this.recipe.getReagent()).toArray(ItemStack[]::new));
        if (!builder.hasTextures()) {
            builder.set(3, 2, new GuiElementBuilder(Items.STAINED_GLASS_PANE.green()).hideTooltip());
        }
        builder.setIngredient(3, 3, stackFromPotionIngredient(this.recipe.getInput()).toArray(ItemStack[]::new));
        builder.setOutput(5, 2, out);

    }

    public static List<ItemStack> stackFromPotionIngredient(PotionIngredient input)  {
        return input.ingredient().items().flatMap(x -> {
            if (input.potions().isEmpty() || input.potions().orElseThrow().potions().isEmpty()) {
                return Stream.of(new ItemStack(x));
            }

            return input.potions().orElseThrow().potions().orElseThrow().stream().map(potion -> {
                var stack = new ItemStack(x);
                stack.set(input.potions().orElseThrow().componentType(), new PotionContents(potion));
                return stack;
            });
        }).toList();
    }
}
