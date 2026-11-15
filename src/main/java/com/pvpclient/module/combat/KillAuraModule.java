package com.pvpclient.module.combat;

import com.pvpclient.module.Category;
import com.pvpclient.module.ClientModule;
import com.pvpclient.setting.BooleanSetting;
import com.pvpclient.setting.ModeSetting;
import com.pvpclient.setting.NumberSetting;
import com.pvpclient.util.AimSmoother;
import com.pvpclient.util.CombatUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public final class KillAuraModule extends ClientModule {
    private final BooleanSetting autoHit = add(new BooleanSetting("Auto Hit", true));
    private final BooleanSetting autoAim = add(new BooleanSetting("Auto Aim", true));
    private final ModeSetting aimAt = add(new ModeSetting("Aim At", 0, "Body", "Head"));
    private final NumberSetting range = add(new NumberSetting("Attack Range", 3.2, 2.0, 4.5, 0.1));
    private final NumberSetting fov = add(new NumberSetting("Attack FOV (no aim)", 180.0, 30.0, 360.0, 5.0));
    private final BooleanSetting onlyCrits = add(new BooleanSetting("Only Crits", true));
    private final BooleanSetting autoJump = add(new BooleanSetting("Auto Jump (for crits)", true));
    private final NumberSetting minInterval = add(new NumberSetting("Min Hit Interval (ticks)", 10.0, 0.0, 20.0, 1.0));
    private final BooleanSetting throughWalls = add(new BooleanSetting("Hit Through Walls", false));

    private LivingEntity target;
    private int sinceHit = 0;
    private int waitedForCrit = 0;

    public KillAuraModule() {
        super("KillAura", "Уничтожает врагов вокруг с тихой наводкой для обхода античитов", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        AimSmoother.resetSilent();
        this.target = null;
        this.waitedForCrit = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            AimSmoother.resetSilent();
            return;
        }
        
        sinceHit++;

        // Поиск подходящей цели по FOV и дистанции
        double attackFov = fov.get();
        LivingEntity victim = CombatUtil.findTarget(mc, range.get(), attackFov, 2, !throughWalls.get(), false);
        this.target = victim;

        if (victim == null) {
            AimSmoother.resetSilent();
            return;
        }

        // --- МАТЕМАТИЧЕСКИЙ РАСЧЕТ ТИХОЙ РОТАЦИИ ДЛЯ СЕРВЕРА (ОБХОД АНТИЧИТА) ---
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d point = aimAt.get() == 1 ? victim.getEyePos() : victim.getBoundingBox().getCenter();
        
        double dX = point.x - eyePos.x;
        double dY = point.y - eyePos.y;
        double dZ = point.z - eyePos.z;
        double horizontal = Math.sqrt(dX * dX + dZ * dZ);
        
        float serverYaw = (float) (Math.toDegrees(Math.atan2(dZ, dX)) - 90.0);
        float serverPitch = (float) (-Math.toDegrees(Math.atan2(dY, horizontal)));

        // Передаем углы в наш пакетный миксин (камера на экране стоит ровно, но сервер видит наводку)
        AimSmoother.setSilentRotation(serverYaw, serverPitch);

        // Авто-прыжок для нанесения критических ударов
        if (onlyCrits.get() && autoJump.get() && mc.player.isOnGround() && mc.player.getVelocity().horizontalLengthSquared() > 0.001) {
            mc.player.jump();
        }

        // Логика задержки перед ударом (Криты)
        boolean ready = sinceHit >= minInterval.get();
        if (!ready) return;

        if (onlyCrits.get()) {
            if (mc.player.isOnGround()) {
                waitedForCrit = 0;
                return; 
            }
            // Проверяем, падает ли игрок вниз для нанесения легитного крита
            if (mc.player.getVelocity().y >= 0.0) {
                waitedForCrit++;
                if (waitedForCrit < 8) return; 
            }
        }

        // Выполняем удар по цели через утилиты
        CombatUtil.attack(mc, victim);
        sinceHit = 0;
        waitedForCrit = 0;
    }

    public static boolean canCrit(ClientPlayerEntity p) {
        return p.fallDistance > 0.0 
            && !p.isOnGround() 
            && !p.isClimbing() 
            && !p.isTouchingWater() 
            && !p.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS) 
            && !p.hasVehicle() 
            && !p.isSprinting();
    }
}