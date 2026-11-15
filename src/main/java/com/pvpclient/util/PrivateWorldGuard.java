package com.pvpclient.util;

import com.pvpclient.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/**
 * Combat and ESP features only run in singleplayer / LAN-host worlds, or on servers whose host
 * you have listed under "authorizedServers" in config/pvpclient.json (servers you own or administer).
 */
public final class PrivateWorldGuard {
    private PrivateWorldGuard() {}

    public static boolean isAuthorized(MinecraftClient mc) {
        if (mc.world == null) return false;
        if (mc.isInSingleplayer()) return true;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return false;
        String host = info.address.trim().toLowerCase();
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(':') == colon) host = host.substring(0, colon);
        for (String allowed : ConfigManager.authorizedServers()) {
            if (allowed.trim().equalsIgnoreCase(host)) return true;
        }
        return false;
    }
}
