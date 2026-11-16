package com.pvpclient.module.combat;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.client.MinecraftClient;

public class VelocityModule {

    private final double horizontalPercent = 0.65;
    private final double verticalPercent = 0.75;

    public boolean onReceivePacket(Object packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return true;

        if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            // ИСПРАВЛЕНО: Используем getEntityId() вместо getId() для Fabric 1.21.4
            if (velocityPacket.getEntityId() == mc.player.getId()) {
                
                int newX = (int) (velocityPacket.getVelocityX() * horizontalPercent);
                int newY = (int) (velocityPacket.getVelocityY() * verticalPercent);
                int newZ = (int) (velocityPacket.getVelocityZ() * horizontalPercent);

                mc.player.setVelocity(
                    (double) newX / 8000.0,
                    (double) newY / 8000.0,
                    (double) newZ / 8000.0
                );
                
                return false;
            }
        }
        return true;
    }
}