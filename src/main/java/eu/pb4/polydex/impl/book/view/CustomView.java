package eu.pb4.polydex.impl.book.view;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Optional;

public final class CustomView implements ItemPageView<CustomView.ViewData> {
    public static CustomView INSTANCE = new CustomView();

    @Override
    public GuiElement getIcon(ItemEntry entry, ViewData object, ServerPlayerEntity player, Runnable returnCallback) {
        var builder = GuiElementBuilder.from(object.icon);
        if (object.name.isPresent()) {
            builder.setName(object.name.get());
            builder.hideFlags();
        }

        if (!object.lore.isEmpty()) {
            builder.hideFlags();
            builder.setLore(object.lore);
        }

        return builder.build();
    }

    @Override
    public void renderLayer(ItemEntry entry, ViewData object, ServerPlayerEntity player, Layer layer, Runnable returnCallback) {
        for (var element : object.elements) {
            if (element.x < 0 || element.y < 0 || element.x > layer.getWidth() || element.y > layer.getWidth()) {
                continue;
            }

            var builder = GuiElementBuilder.from(element.icon);
            if (element.name.isPresent()) {
                builder.setName(object.name.get());
                builder.hideFlags();
            }

            if (!element.lore.isEmpty()) {
                builder.hideFlags();
                builder.setLore(object.lore);
            }

            layer.setSlot(element.x + element.y * layer.getWidth(), builder);
        }
    }

    public record ViewData(Identifier entryId, ItemStack icon, Optional<Text> name, List<Text> lore, List<ItemData> elements) {
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

    public record ItemData(int x, int y, ItemStack icon, Optional<Text> name, List<Text> lore) {
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
