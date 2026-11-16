package com.pvpclient.module.hud;

import com.pvpclient.module.Category;
import com.pvpclient.module.visual.VanishDetectorModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class StaffRadarHud extends HudModule {

    public StaffRadarHud() {
        // Устанавливаем начальное положение окна на экране: x = 15, y = 130 (ниже FPS и CPS)
        super("StaffRadar", "Отображает список модераторов в Ванише", 15.0, 130.0);
    }

    @Override
    protected void draw(DrawContext context) {
        // Окошко будет открываться и рисоваться только тогда, когда найден хоть один админ
        if (VanishDetectorModule.vanishedStaff != null && !VanishDetectorModule.vanishedStaff.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.textRenderer == null) return;

            int width = 145; // Ширина окошка
            int staffSize = VanishDetectorModule.vanishedStaff.size();
            int height = 25 + (staffSize * 13); // Автоматическая высота под количество ников

            // 1. Рисуем красивый темный задний фон (Тёмно-серый с прозрачностью)
            context.fill(0, 0, width, height, 0x95141414);

            // 2. Рисуем стильную фиолетовую полоску-линию на верхней грани окна
            context.fill(0, 0, width, 2, 0xFF8A2BE2);

            // 3. Рисуем заголовок нашего окна
            String title = "👁 STAFF RADAR [" + staffSize + "]";
            context.drawText(mc.textRenderer, title, 7, 7, 0xFFFFFF, true);
            
            // Тонкая разделительная линия под заголовком
            context.fill(5, 18, width - 5, 19, 0x35FFFFFF);

            // 4. Выводим список ников модераторов в столбик
            int currentY = 24;
            for (String staffName : VanishDetectorModule.vanishedStaff) {
                // Префикс [V] пишем ярко-красным цветом, а сам ник — белым
                context.drawText(mc.textRenderer, "[V] ", 8, currentY, 0xFFFF3333, true);
                context.drawText(mc.textRenderer, staffName, 26, currentY, 0xFFF0F0F0, true);
                
                currentY += 13; // Сдвигаем координату вниз для следующей строчки
            }
        }
    }
}