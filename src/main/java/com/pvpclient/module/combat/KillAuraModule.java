package com.pvpclient.module.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillAuraModule {
    
    // Настройки для ReallyWorld (Обход Vulcan/Matrix)
    private final double maxReach = 2.92; // Легитная дистанция удара (строго до 3.0!)
    private final float rotationSpeed = 4.2f; // Скорость доводки камеры
    
    private PlayerEntity target;

    // Этот метод должен вызываться в каждом тике игры (например, из MixinPlayerEntity или ClientTickEvents)
    public void onTick() {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // 1. Поиск ближайшего живого игрока в радиусе видимости
        List<PlayerEntity> targets = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> !p.isDead())
                .filter(p -> p.isAlive())
                .filter(p -> mc.player.distanceTo(p) <= maxReach + 1.0) // Ищем чуть дальше, чтобы начать доводку заранее
                .sorted(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .toList();

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        target = targets.get(0);

        // 2. Расчет идеальных углов до хитбокса цели
        float[] neededRotations = calculateRotations(mc.player.getEyePos(), target.getBoundingBox().getCenter());

        // 3. Плавное сглаживание углов с человеческим шумом (ГЛАВНЫЙ ОБХОД)
        float smoothYaw = smoothAngle(mc.player.getYaw(), neededRotations[0], rotationSpeed);
        float smoothPitch = smoothAngle(mc.player.getPitch(), neededRotations[1], rotationSpeed);

        // Добавляем микро-рандомизацию (шум Гаусса), чтобы античит не видел идеальную математику
        double noise = ThreadLocalRandom.current().nextGaussian() * 0.15;
        smoothYaw += noise;
        smoothPitch += noise;

        // Применяем ротации к игроку (они отправятся на сервер с пакетом движения)
        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(MathHelper.clamp(smoothPitch, -90.0f, 90.0f));

        // 4. Удар (Атака)
        // Бьем, только если цель РЕАЛЬНО в радиусе хитбокса и кулдаун атаки полностью заряжен (1.21.4)
        if (mc.player.distanceTo(target) <= maxReach) {
            if (mc.player.getAttackCooldownProgress(0.5f) >= 0.95f) {
                // Имитируем легитный клик мыши через менеджер взаимодействия
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    // Математический расчет углов взгляда на точку
    private float[] calculateRotations(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double diffXZ = MathHelper.sqrt((float) (diffX * diffX + diffZ * diffZ));

        float yaw = (float) (MathHelper.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(MathHelper.atan2(diffY, diffXZ) * 180.0 / Math.PI));

        return new float[]{yaw, pitch};
    }

    // Алгоритм плавного изменения угла (кинематика)
    private float smoothAngle(float current, float target, float speed) {
        float diff = MathHelper.wrapDegrees(target - current);
        if (diff > speed) diff = speed;
        if (diff < -speed) diff = -speed;
        return current + diff;
    }
}