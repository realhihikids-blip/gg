package com.pvpclient.module.combat;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.client.MinecraftClient;

public class VelocityModule {

    // Настройки обхода для ReallyWorld (Vulcan/Matrix)
    // 0% — это полное отключение отдачи (сразу бан).
    // По горизонтали ставим 65% (тебя почти не отжимают назад, легко комбить).
    // По вертикали ставим 75% (сохраняется естественный подлет вверх при ударе).
    private final double horizontalPercent = 0.65;
    private final double verticalPercent = 0.75;

    // Этот метод должен перехватывать входящие пакеты от сервера (через твой Packet Event или Mixin)
    public boolean onReceivePacket(Object packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return true;

        // Проверяем, пришел ли от сервера пакет об изменении скорости (отдачи)
        if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            
            // Проверяем, что этот пакет прилетел именно нашему игроку
            if (velocityPacket.getId() == mc.player.getId()) {
                
                // ГЛАВНЫЙ ОБХОД: Вместо отмены пакета, мы просто уменьшаем силу толчка
                // Античит видит, что серверный пакет дошел и применился, поэтому не банит
                int newX = (int) (velocityPacket.getVelocityX() * horizontalPercent);
                int newY = (int) (velocityPacket.getVelocityY() * verticalPercent);
                int newZ = (int) (velocityPacket.getVelocityZ() * horizontalPercent);

                // Перезаписываем данные внутри пакета перед тем, как игра их обработает
                // (Для этого в твоем клиенте должны быть настроены аксессоры пакетов/миксины)
                // Если аксессоров нет, скорость можно менять напрямую у игрока в следующем тике:
                mc.player.setVelocity(
                    (double) newX / 8000.0,
                    (double) newY / 8000.0,
                    (double) newZ / 8000.0
                );
                
                return false; // Отменяем стандартную жесткую отдачу
            }
        }
        return true;
    }
}