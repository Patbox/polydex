package eu.pb4.polydex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;

@Mixin(TranslatableContents.class)
public interface TranslatableContentsAccessor {
    @Invoker
    void callDecomposeTemplate(String translation, Consumer<FormattedText> partsConsumer);
}
