package eu.pb4.polydex.impl.book.view;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polydex.api.recipe.PolydexEntry;
import eu.pb4.polydex.api.recipe.PolydexIngredient;
import eu.pb4.polydex.api.recipe.PolydexPage;
import eu.pb4.polydex.api.recipe.PageBuilder;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record CustomPage(Identifier identifier, CustomPage.ViewData object) implements PolydexPage {


    @Override
    public Identifier identifier() {
        return identifier;
    }

    @Override
    public ItemStack getIcon(ServerPlayerEntity player) {
        var builder = GuiElementBuilder.from(object.icon);
        if (object.name.isPresent()) {
            builder.setName(object.name.get());
            builder.hideFlags();
        }

        if (!object.lore.isEmpty()) {
            builder.hideFlags();
            builder.setLore(object.lore);
        }

        return builder.asStack();
    }

    @Override
    public @Nullable Text getTexture(ServerPlayerEntity player) {
        return null;
    }

    @Override
    public void createPage(PolydexEntry entry, ServerPlayerEntity player, PageBuilder b) {
        for (var element : object.elements) {
            if (element.x < 0 || element.y < 0 || element.x > b.width() || element.y > b.height()) {
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

            b.set(element.x, element.y, builder);
        }
    }

    @Override
    public List<PolydexIngredient<?>> getIngredients() {
        return List.of();
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return true;
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
