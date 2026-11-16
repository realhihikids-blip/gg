package com.pvpclient.mixin;

import com.pvpclient.module.visual.FreecamModule;
import com.pvpclient.module.combat.KillAuraModule;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private boolean thirdPerson;
    
    // Переменные для отслеживания углов самой камеры
    @Shadow private float yaw;
    @Shadow private float pitch;

    @Inject(method = "update", at = @At("TAIL"))
    private void pvpclient$freecam(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        // 1. Твоя стандартная Freecam (не трогаем)
        if (FreecamModule.isRunning()) {
            FreecamModule.step();
            setPos(FreecamModule.camX(), FreecamModule.camY(), FreecamModule.camZ());
            setRotation(FreecamModule.camYaw(), FreecamModule.camPitch());
            this.thirdPerson = true;
            return;
        }

        // 2. ИСПРАВЛЕННЫЙ ОБХОД ДЛЯ КИЛЛАУРЫ (Плавный ассист без блокировки мыши)
        if (KillAuraModule.isActive && KillAuraModule.hasTarget) {
            // Вместо жесткого лока камеры мы плавно подтягиваем текущий взгляд игрока (this.yaw)
            // к углам, нужным для удара Киллауры.
            float interpolationSpeed = 0.25f; // Скорость "прилипания" (0.25 — очень плавно и легитно)
            
            float blendYaw = this.yaw + MathHelper.wrapDegrees(KillAuraModule.serverYaw - this.yaw) * interpolationSpeed;
            float blendPitch = this.pitch + (KillAuraModule.serverPitch - this.pitch) * interpolationSpeed;
            
            // Применяем смешанные углы. Экран больше не застынет, ходить можно свободно!
            setRotation(blendYaw, MathHelper.clamp(blendPitch, -90.0f, 90.0f));
        }
    }
}
