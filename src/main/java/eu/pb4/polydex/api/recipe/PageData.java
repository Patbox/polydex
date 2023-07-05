package eu.pb4.polydex.api.recipe;

import eu.pb4.polydex.api.PageView;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public record PageData<T>(Identifier identifier, PageView<T> view, T object) {
    public ItemStack getIcon(ItemEntry entry, ServerPlayerEntity player) {
        try {
            return this.view.getIcon(entry, object, player);
        } catch (Throwable e) {
            e.printStackTrace();
            return PageIcons.INVALID_PAGE;
        }
    }

    public void createPage(ItemEntry entry, PageBuilder builder, ServerPlayerEntity player) {
        try {
            this.view.createPage(entry, this.object, player, builder);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean canDisplay(ItemEntry entry, ServerPlayerEntity player) {
        try {
            return this.view.canDisplay(entry, this.object, player);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Ingredient> getIngredients() {
        try {
            return this.view.getIngredients(this.object);
        } catch (Throwable e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public int priority() {
        return this.view.priority(this.object);
    }
}
