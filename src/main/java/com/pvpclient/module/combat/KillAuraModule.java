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
    
    // Переменная для фиксации цели (Target Lock)
    private AbstractClientPlayerEntity lockedTarget = null;
    
    private int hitTicks = 0;
    private double randomDelay = 0;

    public KillAuraModule() {
        super("KillAura", "Автоматически бьет игроков вокруг", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        isActive = false;
        hasTarget = false;
        lockedTarget = null; // Сброс цели при выключении
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (!this.isEnabled()) {
            isActive = false;
            hasTarget = false;
            lockedTarget = null;
            return;
        }

        hitTicks++;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            isActive = false;
            hasTarget = false;
            lockedTarget = null;
            return;
        }

        // ЛОГИКА TARGET LOCK (Фиксация цели)
        if (lockedTarget != null) {
            // Если цель умерла или убежала слишком далеко — сбрасываем замок цели
            if (lockedTarget.isDead() || !lockedTarget.isAlive() || mc.player.distanceTo(lockedTarget) > maxReach + 1.2) {
                lockedTarget = null;
            }
        }

        if (lockedTarget == null) {
            // Ищем новую цель, если старой нет рядом
            List<AbstractClientPlayerEntity> targets = mc.world.getPlayers().stream()
                    .filter(p -> p != mc.player)
                    .filter(p -> p.isAlive())
                    .filter(p -> mc.player.distanceTo(p) <= maxReach + 1.2)
                    .sorted(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                    .toList();

                    if (!targets.isEmpty()) {
                        // Намертво фиксируем первую найденную цель
                        lockedTarget = targets.get(0);
                    }
        }

        if (lockedTarget == null) {
            hasTarget = false;
            isActive = false;
            return;
        }

        isActive = true;
        hasTarget = true;

        // 1. Расчет чистых углов до хитбокса зафиксированной цели
        float[] neededRotations = calculateRotations(mc.player.getEyePos(), lockedTarget.getBoundingBox().getCenter());

        // 2. Считаем плавный человеческий поворот камеры строго до выбранного игрока
        float smoothYaw = smoothAngle(mc.player.getYaw(), neededRotations[0], rotationSpeed);
        float smoothPitch = smoothAngle(mc.player.getPitch(), neededRotations[1], rotationSpeed);

        // Поворачиваем игрока
        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(MathHelper.clamp(smoothPitch, -90.0f, 90.0f));

        serverYaw = smoothYaw;
        serverPitch = smoothPitch;

        // 3. Строгая проверка угла перед ударом (Защита от киков за Hitboxes)
        float deltaYaw = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - neededRotations[0]));
        float deltaPitch = Math.abs(MathHelper.wrapDegrees(mc.player.getPitch() - neededRotations[1]));

        // Бьем только тогда, когда прицел реально долетел и находится на враге!
        if (deltaYaw > 15.0f || deltaPitch > 15.0f) return;

        // ОБХОД НА ДВИЖЕНИЕ (Защита от проверок "червяков" / застреваний в полублоках)
        // Если во время ПвП ты прыгаешь или трешься об углы блоков, чит шлет пакет ложного заземления, обходя Vulcan
        if (mc.player.verticalCollision || mc.player.horizontalCollision) {
            mc.player.setOnGround(true); 
        }

        // 4. Удар по кулдауну версии 1.21.4 строго по зафиксированной цели
        if (mc.player.distanceTo(lockedTarget) <= maxReach) {
            if (mc.player.getAttackCooldownProgress(0.5f) >= 0.95f && hitTicks >= (8 + randomDelay)) {
                
                if (mc.player.isUsingItem()) {
                    mc.interactionManager.stopUsingItem(mc.player);
                }

                mc.interactionManager.attackEntity(mc.player, lockedTarget);
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