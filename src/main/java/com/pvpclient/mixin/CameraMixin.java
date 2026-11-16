package com.pvpclient.mixin;

import com.pvpclient.module.visual.FreecamModule;
import com.pvpclient.module.combat.KillAuraModule; // Импортируем нашу Киллауру
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
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

    @Inject(method = "update", at = @At("TAIL"))
    private void pvpclient$freecam(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        // 1. Проверяем работу Freecam (твоя старая логика)
        if (FreecamModule.isRunning()) {
            FreecamModule.step();
            setPos(FreecamModule.camX(), FreecamModule.camY(), FreecamModule.camZ());
            setRotation(FreecamModule.camYaw(), FreecamModule.camPitch());
            this.thirdPerson = true;
            return;
        }

        // 2. ГЛАВНЫЙ ОБХОД ДЛЯ КИЛЛАУРЫ (Silent Rotations)
        // Если Киллаура активна и у нее есть цель, мы визуально фиксируем камеру на сглаженных углах обхода
        if (KillAuraModule.isActive && KillAuraModule.hasTarget) {
            setRotation(KillAuraModule.serverYaw, KillAuraModule.serverPitch);
        }
    }
}