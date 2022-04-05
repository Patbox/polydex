package eu.pb4.polydex.impl;

import net.minecraft.server.MinecraftServer;

public interface PolydexServerInterface {
    void polydex_updateTimeReference();

    static void updateTimeReference(MinecraftServer server) {
        ((PolydexServerInterface) server).polydex_updateTimeReference();
    }
}
