package com.pvpclient.module.combat;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.pvpclient.module.ClientModule;
import com.pvpclient.module.Category;
import java.util.Comparator;
import java.util.List;

public class KillAuraModule extends ClientModule {
    
    public static boolean isActive = false;
    public static boolean hasTarget = false;
    public static float serverYaw;
    public static float serverPitch;

    // Легитные настройки для ReallyWorld и FunTime
    private final double maxReach = 2.82; // Безопасная дистанция, чтобы не кикало
    private final float rotationSpeed = 6.5f; // Скорость плавной доводки прицела
    private AbstractClientPlayerEntity target;
    private int hitTicks = 0;
    private double randomDelay = 0;

    public KillAuraModule() {
        super("KillAura", "Автоматически бьет игроков вокруг", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        isActive = false;
        hasTarget = false;
        target = null;
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (!this.isEnabled()) {
            isActive = false;
            hasTarget = false;
            return;
        }

        hitTicks++;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            isActive = false;
            hasTarget = false;
            return;
        }

        // Поиск целей в радиусе удара + запас на доводку
        List<AbstractClientPlayerEntity> targets = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> p.isAlive())
                .filter(p -> mc.player.distanceTo(p) <= maxReach + 1.2)
                .sorted(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .toList();

        if (targets.isEmpty()) {
            target = null;
            hasTarget = false;
            isActive = false;
            return;
        }

        target = targets.get(0);
        isActive = true;
        hasTarget = true;

        // 1. Расчет чистых углов до хитбокса цели
        float[] neededRotations = calculateRotations(mc.player.getEyePos(), target.getBoundingBox().getCenter());

        // 2. ВОЗВРАЩАЕМ НАВОДКУ: Считаем плавный человеческий поворот камеры
        float smoothYaw = smoothAngle(mc.player.getYaw(), neededRotations[0], rotationSpeed);
        float smoothPitch = smoothAngle(mc.player.getPitch(), neededRotations[1], rotationSpeed);

        // Реально поворачиваем самого игрока, чтобы прицел лип к врагу, а удары засчитывались сервером!
        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(MathHelper.clamp(smoothPitch, -90.0f, 90.0f));

        serverYaw = smoothYaw;
        serverPitch = smoothPitch;

        // 3. Строгая проверка угла перед ударом (Защита от киков за Hitboxes)
        float deltaYaw = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - neededRotations[0]));
        float deltaPitch = Math.abs(MathHelper.wrapDegrees(mc.player.getPitch() - neededRotations[1]));

        // Бьем только тогда, когда прицел реально долетел и находится на враге!
        if (deltaYaw > 15.0f || deltaPitch > 15.0f) return;

        // 4. Удар по кулдауну версии 1.21.4
        if (mc.player.distanceTo(target) <= maxReach) {
            if (mc.player.getAttackCooldownProgress(0.5f) >= 0.95f && hitTicks >= (8 + randomDelay)) {
                
                if (mc.player.isUsingItem()) {
                    mc.interactionManager.stopUsingItem(mc.player);
                }

                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                
                randomDelay = Math.random() * 4.0;
                hitTicks = 0;
            }
        }
    }

    private float[] calculateRotations(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double diffXZ = MathHelper.sqrt((float) (diffX * diffX + diffZ * diffZ));
        float yaw = (float) (MathHelper.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(MathHelper.atan2(diffY, diffXZ) * 180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    private float smoothAngle(float current, float target, float speed) {
        float diff = MathHelper.wrapDegrees(target - current);
        if (diff > speed) diff = speed;
        if (diff < -speed) diff = -speed;
        return current + diff;
    }
}
