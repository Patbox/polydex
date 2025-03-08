package eu.pb4.polydex.mixin;

import net.minecraft.text.StringVisitable;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(TranslatableTextContent.class)
public interface TranslatableTextContentAccessor {
    @Invoker
    void callForEachPart(String translation, Consumer<StringVisitable> partsConsumer);
}
