package eu.pb4.polydex.impl.book.view;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ToolUseOnBlockPage(Identifier identifier, PolydexIngredient<ItemStack> tool, PolydexIngredient<ItemStack> from, PolydexStack<ItemStack> to) implements PolydexPage {
    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        //noinspection unchecked
        return GuiElementBuilder.from(tool.asFirstStack().orElse((PolydexStack<ItemStack>) PolydexStack.EMPTY).toTypeDisplayItemStack(player))
                .hideDefaultTooltip()
                .setName(Component.translatable("polydex_category.minecraft.tool_interaction"))
                .asStack();

    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        return to.toTypeDisplayItemStack(player);
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder layer) {
        layer.setIngredient(2, 2, from);
        if (!layer.hasTextures()) {
            layer.set(4, 2, new GuiElementBuilder(Items.ARROW).hideTooltip());
        }
        layer.setIngredient(4, 3, tool, (b) -> b.hideDefaultTooltip().setName(Component.translatable("text.polydex.use_tool_on_block").withStyle(ChatFormatting.GOLD)));

        layer.setOutput(6, 2, to);
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
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
