package com.pvpclient.util;

import net.minecraft.util.math.MathHelper;

public final class AimSmoother {
    private double yaw, pitch;
    private boolean ready;

    // ПЕРЕМЕННЫЕ ДЛЯ ТИХОГО РЕЖИМА (Silent)
    public static float silentYaw;
    public static float silentPitch;
    public static boolean isSilentRotating = false;

    // Включает тихую ротацию (сервер видит углы, а твой экран — нет)
    public static void setSilentRotation(float yaw, float pitch) {
        silentYaw = yaw;
        silentPitch = pitch;
        isSilentRotating = true;
    }

    // Выключает тихую ротацию
    public static void resetSilent() {
        isSilentRotating = false;
    }

    // --- Оригинальный код Клода (сохраняем, чтобы ничего не сломать) ---
    public void snap(float targetYaw, float targetPitch) {
        this.yaw = targetYaw;
        this.pitch = targetPitch;
        ready = true;
    }

    public void reset() { ready = false; }

    public void towards(float targetYaw, float targetPitch, double rate, double dt) {
        if (!ready) { snap(targetYaw, targetPitch); return; }
        double factor = 1.0 - Math.exp(-Math.max(0, dt) * Math.max(0.01, rate));
        yaw += MathHelper.wrapDegrees((float) (targetYaw - yaw)) * factor;
        pitch += (targetPitch - pitch) * factor;
    }

    public float getYaw() { return (float) yaw; }
    public float getPitch() { return (float) pitch; }
}