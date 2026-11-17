package com.pvpclient.module.combat;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.pvpclient.module.ClientModule;
import com.pvpclient.module.Category;
import java.util.Comparator;
import java.util.List;

public class CrystalAuraModule extends ClientModule {

    private final double range = 4.0;             
    private final double maxSelfDamage = 6.0;     
    private final double minEnemyDamage = 10.0;   
    
    private int crystalTicks = 0;

    public CrystalAuraModule() {
        super("CrystalAura", "Автоматически ставит и взрывает кристаллы с просчетом безопасного урона", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (!this.isEnabled()) return;

        crystalTicks++;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.interactionManager == null) return;

        AbstractClientPlayerEntity enemy = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);

        if (enemy == null || mc.player.distanceTo(enemy) > range + 2.0) return;

        EndCrystalEntity crystal = mc.world.getEntitiesByClass(EndCrystalEntity.class, 
                new Box(enemy.getBlockPos()).expand(range), 
                e -> e.isAlive() && mc.player.distanceTo(e) <= range)
                .stream()
                .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                .orElse(null);

        if (crystal != null && crystalTicks >= 2) { 
            double selfDmg = calculateExplosionDamage(crystal.getPos(), mc.player);
            double enemyDmg = calculateExplosionDamage(crystal.getPos(), enemy);

            if (selfDmg <= maxSelfDamage && enemyDmg >= minEnemyDamage) {
                mc.interactionManager.attackEntity(mc.player, crystal);
                mc.player.swingHand(Hand.MAIN_HAND);
                crystalTicks = 0;
                return;
            }
        }

        boolean holdsCrystal = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL 
                            || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        
        if (!holdsCrystal) return;

        BlockPos bestPos = null;
        double bestEnemyDmg = 0;

        Iterable<BlockPos> blocks = BlockPos.iterate(
                enemy.getBlockPos().add(-3, -2, -3), 
                enemy.getBlockPos().add(3, 1, 3)
        );

        for (BlockPos pos : blocks) {
            var blockType = mc.world.getBlockState(pos).getBlock();
            if (blockType == net.minecraft.block.Blocks.OBSIDIAN || blockType == net.minecraft.block.Blocks.BEDROCK) {
                
                if (mc.world.isAir(pos.up())) {
                    // ИСПРАВЛЕНО: Заменили Vec3p на Vec3d
                    Vec3d crystalVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    
                    double selfDmg = calculateExplosionDamage(crystalVec, mc.player);
                    double enemyDmg = calculateExplosionDamage(crystalVec, enemy);

                    if (selfDmg <= maxSelfDamage && enemyDmg >= minEnemyDamage) {
                        if (enemyDmg > bestEnemyDmg) {
                            bestEnemyDmg = enemyDmg;
                            bestPos = pos.toImmutable();
                        }
                    }
                }
            }
        }

        if (bestPos != null && crystalTicks >= 2) {
            Hand hand = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL ? Hand.MAIN_HAND : Hand.OFF_HAND;
            BlockHitResult hitResult = new BlockHitResult(
                    new Vec3d(bestPos.getX() + 0.5, bestPos.getY() + 1.0, bestPos.getZ() + 0.5), 
                    Direction.UP, 
                    bestPos, 
                    false
            );
            mc.interactionManager.interactBlock(mc.player, hand, hitResult);
            crystalTicks = 0;
        }
    }

    private double calculateExplosionDamage(Vec3d crystalPos, net.minecraft.entity.LivingEntity entity) {
        // ИСПРАВЛЕНО: Используем entity.getWorld() вместо entity.world
        double distance = entity.distanceTo(new EndCrystalEntity(entity.getWorld(), crystalPos.x, crystalPos.y, crystalPos.z));
        double maxDist = 12.0; 
        
        if (distance > maxDist) return 0.0;

        double exposure = (maxDist - distance) / maxDist;
        double baseDamage = ((exposure * exposure + exposure) / 2.0) * 7.0 * 12.0 + 1.0;
        return baseDamage * 0.3; 
    }
}
