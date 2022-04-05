package eu.pb4.polydex.mixin;

import eu.pb4.polydex.impl.PolydexServerInterface;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements PolydexServerInterface {

    @Shadow private long timeReference;

    @Override
    public void polydex_updateTimeReference() {
        this.timeReference = Util.getMeasuringTimeMs();
    }
}
