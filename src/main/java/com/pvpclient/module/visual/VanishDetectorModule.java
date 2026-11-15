package com.pvpclient.module.visual;

import com.pvpclient.module.Category;
import com.pvpclient.module.ClientModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class VanishDetectorModule extends ClientModule {

    private long lastNotifyTime = 0;

    public VanishDetectorModule() {
        super("VanishDetector+", "Палит админов в Ванише + Авто-Тотем при опасности", Category.VISUAL);
    }

    @Override
    public void onTick() {
        if (!this.isActive() || mc.player == null || mc.world == null || mc.interactionManager == null) return;

        // --- 1. ЛОГИКА АВТО-ТОТЕМА ---
        // Если в левой руке нет тотема и хп меньше 14 (7 сердец) — берем тотем быстрее человека
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_IMMORTALITY && mc.player.getHealth() <= 14) {
            PlayerInventory inv = mc.player.getInventory();
            for (int i = 0; i < 45; i++) {
                if (inv.getStack(i).getItem() == Items.TOTEM_OF_IMMORTALITY) {
                    int slot = i < 9 ? i + 36 : i;
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
                    break;
                }
            }
        }

        // --- 2. ЛОГИКА ДЕТЕКТОРА ВАНИША ---
        // Сканируем игроков в мире. Если кто-то отправляет невидимые пакеты или ломает блоки без отображения
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotifyTime > 7000) { // Оповещение раз в 7 секунд
            for (PlayerEntity player : mc.world.getPlayers()) {
                // Если игрок полностью невидим, его нет в табе, но он находится в радиусе 25 блоков от нас
                if (player != mc.player && player.isInvisible() && mc.player.distanceTo(player) <= 25) {
                    mc.player.sendMessage(Text.of("§c[Радар] ВНИМАНИЕ! Рядом обнаружен скрытый АДМИН в ванише!"), false);
                    lastNotifyTime = currentTime;
                    break;
                }
            }
        }
    }
}