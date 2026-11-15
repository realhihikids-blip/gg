package com.pvpclient.module.combat;

import com.pvpclient.module.Category;
import com.pvpclient.module.ClientModule;
import com.pvpclient.setting.BooleanSetting;
import com.pvpclient.setting.ModeSetting;
import com.pvpclient.setting.NumberSetting;
import com.pvpclient.util.AimSmoother;
import com.pvpclient.util.Anim;
import com.pvpclient.util.CombatUtil;
import com.pvpclient.util.ItemUser;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/**
 * Practice aura for private test worlds: auto-aim, auto-follow and auto-hit.
 * Aiming turns your real camera (visible on screen); nothing is spoofed or hidden from the server.
 */
public final class KillAuraModule extends ClientModule {
    private final BooleanSetting autoHit = add(new BooleanSetting("Auto Hit", true));
    private final BooleanSetting autoAim = add(new BooleanSetting("Auto Aim", true));
    private final ModeSetting aimAt = add(new ModeSetting("Aim At", 0, "Body", "Head"));
    private final NumberSetting aimSpeed = add(new NumberSetting("Aim Speed (deg/s)", 540.0, 90.0, 1440.0, 30.0));
    private final BooleanSetting smoothTurning = add(new BooleanSetting("Smooth Camera", true));
    private final NumberSetting smoothRate = add(new NumberSetting("Smoothing Speed", 10.0, 2.0, 30.0, 1.0));
    private final BooleanSetting autoFollow = add(new BooleanSetting("Auto Follow", true));
    private final BooleanSetting autoSprint = add(new BooleanSetting("Auto Sprint", true));
    private final NumberSetting targetRange = add(new NumberSetting("Target Range", 16.0, 4.0, 32.0, 1.0));
    private final NumberSetting stopDistance = add(new NumberSetting("Follow Stop Distance", 2.4, 1.5, 4.0, 0.1));
    private final NumberSetting range = add(new NumberSetting("Attack Range", 3.0, 2.0, 4.5, 0.1));
    private final NumberSetting fov = add(new NumberSetting("Attack FOV (no aim)", 100.0, 30.0, 360.0, 5.0));
    private final NumberSetting charge = add(new NumberSetting("Cooldown Charge", 0.95, 0.5, 1.0, 0.05));
    private final BooleanSetting onlyCrits = add(new BooleanSetting("Only Crits", true));
    private final BooleanSetting autoJump = add(new BooleanSetting("Auto Jump (for crits)", true));
    private final BooleanSetting strictCrits = add(new BooleanSetting("Strict Crits", true));
    private final NumberSetting critTimeout = add(new NumberSetting("Crit Timeout (ticks)", 30.0, 10.0, 80.0, 5.0));
    private final NumberSetting minInterval = add(new NumberSetting("Min Hit Interval (ticks)", 10.0, 0.0, 20.0, 1.0));
    private final ModeSetting targets = add(new ModeSetting("Targets", 2, CombatUtil.TARGET_MODES));
    private final BooleanSetting circleStrafe = add(new BooleanSetting("Circle Strafe", false));
    private final NumberSetting strafeSwitch = add(new NumberSetting("Strafe Switch (ticks)", 30.0, 0.0, 100.0, 5.0));
    private final BooleanSetting throughWalls = add(new BooleanSetting("Hit Through Walls", false));

    private LivingEntity target;
    private final AimSmoother smoother = new AimSmoother();
    private int sinceHit = 100;
    private int waitedForCrit = 0;
    private int strafeDir = 1;
    private int strafeTicks = 0;
    private final Set<KeyBinding> pressedByUs = new HashSet<>();

    public KillAuraModule() {
        super("KillAura", "Practice mode: auto-aim, follow and hit (private worlds only)", Category.COMBAT);
        markRestricted();
    }

    @Override protected void onDisable() { release(); smoother.reset(); }
    @Override public void onIdle() { if (!pressedByUs.isEmpty() || target != null) release(); }

    private boolean aiming() { return autoAim.get() || autoFollow.get() || circleStrafe.get(); }

    // ---------------------------------------------------------------- tick: pick target, follow, hit

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) { release(); return; }
        if (mc.currentScreen != null || p.isUsingItem() || ItemUser.busy()) { release(); return; }
        sinceHit++;

        // nearest valid target (any direction when aiming/following)
        boolean wide = aiming();
        double search = wide ? targetRange.get() : range.get();
        target = CombatUtil.findTarget(mc, search, wide ? 360.0 : fov.get(), targets.get(), false);

        // who can be hit right now
        double attackFov = autoAim.get() ? 60.0 : fov.get();
        LivingEntity victim = CombatUtil.findTarget(mc, range.get(), attackFov, targets.get(), !throughWalls.get());
        boolean crits = onlyCrits.get();
        float cooldown = p.getAttackCooldownProgress(0.0f);

        // movement: follow the target, jump for crits
        boolean forward = false, sprint = false, jump = false;
        if (autoFollow.get() && target != null) {
            double dist = CombatUtil.distanceToBox(p.getEyePos(), target);
            forward = dist > stopDistance.get();
            // sprinting blocks critical hits, so stop sprinting when close in crit mode
            boolean close = crits && dist <= range.get() + 1.5;
            sprint = forward && autoSprint.get() && dist > stopDistance.get() + 1.5 && !close;
            jump = forward && p.horizontalCollision && p.isOnGround();
        }
        // circle strafe: keep moving sideways around the target while facing it
        boolean left = false, right = false, back = false;
        if (circleStrafe.get() && target != null) {
            double dist = CombatUtil.distanceToBox(p.getEyePos(), target);
            if (dist <= range.get() + 3.0) {
                strafeTicks++;
                boolean timeUp = strafeSwitch.get() > 0 && strafeTicks >= strafeSwitch.get();
                boolean blocked = p.horizontalCollision && strafeTicks > 5;
                if (timeUp || blocked) { strafeDir = -strafeDir; strafeTicks = 0; }
                right = strafeDir > 0;
                left = strafeDir < 0;
                back = dist < stopDistance.get() * 0.7;
                forward = !back && dist > stopDistance.get();
            }
        }
        if (crits && autoJump.get() && victim != null && p.isOnGround() && cooldown >= 0.5f) jump = true;
        // sprinting blocks crits, and the server only learns about it next tick: stop now, hit later
        boolean stoppedSprint = false;
        if (crits && victim != null && p.isSprinting()) {
            p.setSprinting(false);
            stoppedSprint = true;
        }
        setKey(mc.options.forwardKey, forward);
        setKey(mc.options.sprintKey, sprint);
        setKey(mc.options.jumpKey, jump);
        setKey(mc.options.leftKey, left);
        setKey(mc.options.rightKey, right);
        setKey(mc.options.backKey, back);

        // hit: once per cooldown, and (in crit mode) only while falling
        if (!autoHit.get() || victim == null) { waitedForCrit = 0; return; }
        boolean ready = cooldown >= charge.get() && sinceHit >= minInterval.get();
        if (!ready) return;

        if (crits) {
            if (stoppedSprint) return;
            if (!canCrit(p)) {
                waitedForCrit++;
                // strict: never hit without a crit; otherwise fall back to a normal hit after the timeout
                if (strictCrits.get() || waitedForCrit < critTimeout.get()) return;
            }
        }
        CombatUtil.attack(mc, victim);
        sinceHit = 0;
        waitedForCrit = 0;
    }

    /** Vanilla critical-hit conditions: falling, not on ground, not climbing/swimming/blind/riding, not sprinting. */
    private static boolean canCrit(ClientPlayerEntity p) {
        return p.fallDistance > 0.0
                && !p.isOnGround()
                && !p.isClimbing()
                && !p.isTouchingWater()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS)
                && !p.hasVehicle()
                && !p.isSprinting();
    }

    // ---------------------------------------------------------------- frame: smooth camera turn

    @Override
    public void onWorldRender(WorldRenderContext ctx) {
        ClientPlayerEntity p = mc.player;
        if (p == null || target == null || !target.isAlive() || mc.currentScreen != null) { smoother.reset(); return; }
        if (!aiming()) { smoother.reset(); return; }

        Vec3d eye = p.getEyePos();
        Vec3d point = aimAt.get() == 1 ? target.getEyePos() : target.getBoundingBox().getCenter();
        double dx = point.x - eye.x, dy = point.y - eye.y, dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        if (smoothTurning.get()) {
            // a large jump (e.g. the target just switched) snaps once instead of sweeping across the screen
            if (Math.abs(MathHelper.wrapDegrees(wantYaw - p.getYaw())) > 90.0) smoother.snap(p.getYaw(), p.getPitch());
            smoother.towards(wantYaw, wantPitch, smoothRate.get(), Anim.dt);
            p.setYaw(smoother.yaw());
            p.setPitch(smoother.pitch());
        } else {
            smoother.reset();
            float dYaw = MathHelper.wrapDegrees(wantYaw - p.getYaw());
            float dPitch = wantPitch - p.getPitch();
            float step = (float) (aimSpeed.get() * Anim.dt);
            p.setYaw(p.getYaw() + MathHelper.clamp(dYaw, -step, step));
            p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dPitch, -step, step), -90.0f, 90.0f));
        }
    }

    // ---------------------------------------------------------------- key handling

    private void setKey(KeyBinding key, boolean down) {
        if (down) {
            key.setPressed(true);
            pressedByUs.add(key);
        } else if (pressedByUs.remove(key)) {
            key.setPressed(false);
        }
    }

    /** Releases only the keys this module pressed, so nothing stays stuck. */
    private void release() {
        for (KeyBinding k : pressedByUs) k.setPressed(false);
        pressedByUs.clear();
        target = null;
    }
}
