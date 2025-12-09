package eu.pb4.polydex.impl.book.view;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record CustomPage(Identifier identifier, CustomPage.ViewData object) implements PolydexPage {


    @Override
    public Identifier identifier() {
        return identifier;
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        var builder = GuiElementBuilder.from(object.icon);
        if (object.name.isPresent()) {
            builder.setName(object.name.get());
            builder.hideDefaultTooltip();
        }

        if (!object.lore.isEmpty()) {
            builder.hideDefaultTooltip();
            builder.setLore(object.lore);
        }

        return builder.asStack();
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        return entry != null ? entry.stack().toTypeDisplayItemStack(player) : ItemStack.EMPTY;
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return null;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayer player, PageBuilder b) {
        for (var element : object.elements) {
            if (element.x < 0 || element.y < 0 || element.x > b.width() || element.y > b.height()) {
                continue;
            }

            var builder = GuiElementBuilder.from(element.icon);
            if (element.name.isPresent()) {
                builder.setName(object.name.get());
                builder.hideDefaultTooltip();
            }

            if (!element.lore.isEmpty()) {
                builder.hideDefaultTooltip();
                builder.setLore(object.lore);
            }

            b.set(element.x, element.y, builder);
        }
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return List.of();
    }

    @Override
    public List<PolydexCategory> categories() {
        return List.of(PolydexCategory.CUSTOM);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.identifier().equals(this.object.entryId);
    }

    public record ViewData(Identifier entryId, ItemStack icon, Optional<Component> name, List<Component> lore, List<ItemData> elements) {
        public static Codec<ViewData> CODEC = RecordCodecBuilder.create(
                (instance) -> instance.group(
                        Identifier.CODEC.fieldOf("entry").forGetter(ViewData::entryId),
                        PolydexImpl.ITEM_STACK_CODEC.fieldOf("icon").forGetter(ViewData::icon),
                        PolydexImpl.TEXT.optionalFieldOf("name").forGetter(ViewData::name),
                        Codec.list(PolydexImpl.TEXT).optionalFieldOf("lore", List.of()).forGetter(ViewData::lore),
                        Codec.list(ItemData.CODEC).optionalFieldOf("elements", List.of()).forGetter(ViewData::elements)
                ).apply(instance, ViewData::new)
        );
    }

    public record ItemData(int x, int y, ItemStack icon, Optional<Component> name, List<Component> lore) {
        public static Codec<ItemData> CODEC = RecordCodecBuilder.create(
                (instance) -> instance.group(
                        Codec.INT.fieldOf("x").forGetter(ItemData::x),
                        Codec.INT.fieldOf("y").forGetter(ItemData::y),
                        PolydexImpl.ITEM_STACK_CODEC.fieldOf("icon").forGetter(ItemData::icon),
                        PolydexImpl.TEXT.optionalFieldOf("name").forGetter(ItemData::name),
                        Codec.list(PolydexImpl.TEXT).optionalFieldOf("lore", List.of()).forGetter(ItemData::lore)
                ).apply(instance, ItemData::new)
        );
    }
}
