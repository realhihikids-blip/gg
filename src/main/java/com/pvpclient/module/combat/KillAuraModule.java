package com.pvpclient.module.combat;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.pvpclient.module.ClientModule; // Импорт базового класса модуля
import com.pvpclient.module.Category; // Импорт категорий, если нужно
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillAuraModule extends ClientModule {
    
    // Переменные для CameraMixin и Silent наводки
    public static boolean isActive = false;
    public static boolean hasTarget = false;
    public static float serverYaw;
    public static float serverPitch;

    private final double maxReach = 2.92;
    private final float rotationSpeed = 4.2f;
    private AbstractClientPlayerEntity target;
    private int hitTicks = 0;
    private double randomDelay = 0;

    // Конструктор модуля (исправляет ошибку регистрации)
    public KillAuraModule() {
        super("KillAura", "Автоматически бьет игроков вокруг", Category.COMBAT);
    }
@Override
    public void onDisable() {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null) {
            serverYaw = mc.player.getYaw();
            serverPitch = mc.player.getPitch();
        }
        isActive = false;
        hasTarget = false;
        target = null;
        super.onDisable();
    }
    
    @Override
    public void onTick() {
        hitTicks++;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            isActive = false;
            hasTarget = false;
            return;
        }

        // Ищем игроков с правильным типом данных Fabric
        List<AbstractClientPlayerEntity> targets = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> p.isAlive())
                .filter(p -> mc.player.distanceTo(p) <= maxReach + 1.0)
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

        // Расчет углов
        float[] neededRotations = calculateRotations(mc.player.getEyePos(), target.getBoundingBox().getCenter());

        // Сглаживание
        float smoothYaw = smoothAngle(mc.player.getYaw(), neededRotations[0], rotationSpeed);
        float smoothPitch = smoothAngle(mc.player.getPitch(), neededRotations[1], rotationSpeed);

        double noise = ThreadLocalRandom.current().nextGaussian() * 0.15;
        serverYaw = smoothYaw + (float) noise;
        serverPitch = MathHelper.clamp(smoothPitch + (float) noise, -90.0f, 90.0f);

        // Удар с обходом ReallyWorld
        if (mc.player.distanceTo(target) <= maxReach) {
            if (mc.player.getAttackCooldownProgress(0.5f) >= 0.95f && hitTicks >= (10 + randomDelay)) {
                
                if (mc.player.isUsingItem()) {
                    mc.interactionManager.stopUsingItem(mc.player);
                }

                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                
                randomDelay = Math.random() * 3.0;
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
