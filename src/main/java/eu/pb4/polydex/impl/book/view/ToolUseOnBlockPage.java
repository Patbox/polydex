package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ToolUseOnBlockPage(Identifier identifier, PolydexIngredient<ItemStack> tool, PolydexIngredient<ItemStack> from, PolydexStack<ItemStack> to) implements PolydexPage {
    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        //noinspection unchecked
        return GuiElementBuilder.from(tool.asFirstStack().orElse((PolydexStack<ItemStack>) PolydexStack.EMPTY).toTypeDisplayItemStack(player))
                .hideDefaultTooltip()
                .setName(Text.translatable("polydex_category.minecraft.tool_interaction"))
                .asStack();

    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return to.toTypeDisplayItemStack(player);
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        layer.setIngredient(2, 2, from);
        if (!layer.hasTextures()) {
            layer.set(4, 2, new GuiElementBuilder(Items.ARROW).hideTooltip());
        }
        layer.setIngredient(4, 3, tool, (b) -> b.hideDefaultTooltip().setName(Text.translatable("text.polydex.use_tool_on_block").formatted(Formatting.GOLD)));

        layer.setOutput(6, 2, to);
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return InternalPageTextures.SIMPLE_TRANSFORM;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return List.of(from);
    }

    @Override
    public List<PolydexCategory> categories() {
        return List.of(PolydexCategory.TOOL_INTERACTION);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.isPartOf(to);
    }
}
