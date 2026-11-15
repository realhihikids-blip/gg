package com.pvpclient.util;

import net.minecraft.util.math.MathHelper;

/**
 * Eases the real, visible camera rotation toward a target instead of stepping it linearly every frame.
 * This only changes how smoothly your actual (server-visible) yaw/pitch animates; the angle applied is
 * still the true rotation, exactly what is shown on screen and sent to the server.
 */
public final class AimSmoother {
    private double yaw, pitch;
    private boolean ready;

    /** Jumps straight to an angle with no easing (used on enable or when the target changes a lot). */
    public void snap(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        ready = true;
    }

    public void reset() { ready = false; }

    /**
     * Eases toward the target. If not yet initialized, snaps instead (avoids a sweep from 0,0 on first use).
     * @param rate  how quickly the ease catches up, in 1/s (higher = snappier, lower = smoother)
     */
    public void towards(float targetYaw, float targetPitch, double rate, double dt) {
        if (!ready) { snap(targetYaw, targetPitch); return; }
        double factor = 1.0 - Math.exp(-Math.max(0, dt) * Math.max(0.01, rate));
        yaw += MathHelper.wrapDegrees((float) (targetYaw - yaw)) * factor;
        pitch += (targetPitch - pitch) * factor;
        pitch = MathHelper.clamp(pitch, -90.0, 90.0);
    }

    public float yaw() { return MathHelper.wrapDegrees((float) yaw); }
    public float pitch() { return (float) pitch; }
}
