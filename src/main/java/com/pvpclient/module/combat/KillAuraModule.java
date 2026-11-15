package com.pvpclient.module.combat;

import com.pvpclient.module.Category;
import com.pvpclient.module.ClientModule;
import com.pvpclient.setting.BooleanSetting;
import com.pvpclient.setting.NumberSetting;
import com.pvpclient.util.AimSmoother;
import com.pvpclient.util.CombatUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class KillAuraModule extends ClientModule {
    private final NumberSetting range = add(new NumberSetting("Attack Range", 3.1, 2.0, 4.5, 0.1));
    private final NumberSetting fov = add(new NumberSetting("Attack FOV", 120.0, 30.0, 360.0, 5.0));
    private final NumberSetting smoothSpeed = add(new NumberSetting("Aim Smooth", 15.0, 5.0, 30.0, 1.0));
    private final BooleanSetting onlyCrits = add(new BooleanSetting("Only Crits", true));

    private final AimSmoother smoother = new AimSmoother();
    private int hitTicks = 0;
    private double randomDelay = 0;

    public KillAuraModule() {
        super("KillAura", "Плавная киллаура с рандомизацией для обхода Matrix/Vulcan", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        smoother.reset();
        hitTicks = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        hitTicks++;

        // 1. Ищем цель (передаем 5 параметров под твой сурс)
        LivingEntity victim = CombatUtil.findTarget(mc, range.get(), fov.get(), 0, true);
        if (victim == null) {
            smoother.reset();
            return;
        }

        // 2. Рассчитываем углы до хитбокса врага
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPoint = victim.getBoundingBox().getCenter();
        
        double dX = targetPoint.x - eyePos.x;
        double dY = targetPoint.y - eyePos.y;
        double dZ = targetPoint.z - eyePos.z;
        double horizontal = Math.sqrt(dX * dX + dZ * dZ);
        
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dZ, dX)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dY, horizontal)));

        // 3. Плавное легитное наведение камеры (без дёрганий и крашей пакетов)
        smoother.towards(targetYaw, targetPitch, smoothSpeed.get(), 0.05);
        
        mc.player.setYaw(smoother.getYaw());
        mc.player.setPitch(smoother.getPitch());

        // 4. Проверяем, наведён ли прицел близко к хитбоксу перед атакой
        float deltaYaw = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - targetYaw));
        if (deltaYaw > 35.0f) return; 

        // 5. Рандомизация ударов (плавающий кулдаун для обмана Matrix)
        if (hitTicks >= (10 + randomDelay)) {
            if (onlyCrits.get() && mc.player.isOnGround()) {
                mc.player.jump();
                return;
            }

            // Наносим удар
            CombatUtil.attack(mc, victim);
            
            // Задаем случайное смещение для следующего удара
            randomDelay = Math.random() * 3.0; 
            hitTicks = 0;
        }
    }
}