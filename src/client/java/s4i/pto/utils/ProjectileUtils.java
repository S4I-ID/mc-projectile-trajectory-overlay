package s4i.pto.utils;


import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import s4i.pto.model.Constants;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.model.projectile.ProjectileSpawnData;

import static s4i.pto.model.Constants.DEG_TO_RAD;
import static s4i.pto.model.Constants.RAD_TO_DEG;

public class ProjectileUtils {
    /**
     * Most player thrown projectiles spawn at eye level minus 0.1f
     * @param tickProgress needed to interpolate entity camera position vector between ticks
     * @param firingEntity entity doing the firing
     * @return projectile estimated spawning position as {@link Vec3 Vec3d(x,y,z)}
     */
    public static Vec3 calculateStartingPosition(float tickProgress, LivingEntity firingEntity) {
        return firingEntity.getEyePosition(tickProgress);
    }

    /**
     * Crossbows spawn their projectiles at eye level minus 0.15f
     * @see CrossbowItem#createProjectile(Level, LivingEntity, ItemStack, ItemStack, boolean) 
     */
    public static Vec3 calculateCrossbowStartingPosition(float tickProgress, LivingEntity firingEntity) {
        return firingEntity.getEyePosition(tickProgress);
    }

    /**
     * Fishing bobbers take entity's yaw into consideration
     * @see FishingHook#FishingHook(Player player, Level level, int luck, int lureSpeed)
     */
    public static Vec3 calculateFishingBobberStartingPosition(float tickProgress, LivingEntity firingEntity) {
        float g = firingEntity.getYRot(tickProgress);
        float h = Mth.cos(-g * DEG_TO_RAD - Mth.PI);
        float i = Mth.sin(-g * DEG_TO_RAD - Mth.PI);
        return firingEntity.getEyePosition(tickProgress).add(-i * 0.3, -0.1f, -h * 0.3);
    }

    /**
     * Normalizes given value to a given max value. Used for coloring lines in this mod
     */
    public static float toQuadraticScale(float value, float maxValue) {
        float result = value / maxValue;
        return result * result;
    }

    /**
     * Estimates spawned projectile position, velocity and other data necessary for prediction
     * @param tickProgress progress of tick to interpolate entity yaw/pitch inbetween ticks for smooth line rendering
     * @param firingEntity entity which fires the projectile
     * @param speed also called power in internal code, hardcoded for most projectiles/weapons
     * @param divergence randomized spread used in {@link Projectile#shoot(double, double, double, float, float)}, for players it is set to 1.0f
     * @param roll certain thrown items like {@link ThrowablePotionItem#use(Level, Player, InteractionHand)} use this and multi-shot crossbows
     * @return simulated projectile data to be used for estimations
     */
    public static ProjectileSpawnData calculateRangedWeaponItemStartingVelocity(float tickProgress, Player firingEntity, float speed, float divergence, float roll) {
        float yaw = firingEntity.getYRot(tickProgress);
        float pitch = firingEntity.getXRot(tickProgress);
        float xVector = -Mth.sin(yaw * DEG_TO_RAD) * Mth.cos(pitch * DEG_TO_RAD);
        float yVector = -Mth.sin((pitch + roll) * DEG_TO_RAD);
        float zVector = Mth.cos(yaw * DEG_TO_RAD) * Mth.cos(pitch * DEG_TO_RAD);
        Vec3 initialVelocity = new Vec3(xVector, yVector, zVector);
        Vec3 velocity = firingEntity.getViewVector(tickProgress).normalize().scale(speed);
        Vec3 initialFiringEntityVelocity = EntityUtils.getEntityServerMovement(firingEntity);
        Vec3 finalFiringEntityVelocity = firingEntity.onGround() || firingEntity.getVehicle() != null ?
                initialFiringEntityVelocity.multiply(1, 0, 1) : initialFiringEntityVelocity;
        Vec3 finalVelocity = velocity.add(finalFiringEntityVelocity);
        return new ProjectileSpawnData()
                .setVelocity(finalVelocity)
                .setVelocityWithMaxNegativeDeviation(initialVelocity.normalize().add(-Constants.MAX_DEVIATION * divergence).scale(speed).add(finalFiringEntityVelocity))
                .setVelocityWithMaxPositiveDeviation(initialVelocity.normalize().add(Constants.MAX_DEVIATION * divergence).scale(speed).add(finalFiringEntityVelocity))
                .setYaw((float) (Mth.atan2(velocity.x, velocity.z) * RAD_TO_DEG))
                .setPitch((float) (Mth.atan2(velocity.y, velocity.horizontalDistance()) * RAD_TO_DEG));
    }

    /**
     * Crossbows do not take player or vehicle movement into consideration
     * @see CrossbowItem#shoot
     */
    public static ProjectileSpawnData calculateCrossbowStartingVelocity(Vec3 firingEntityViewVector, float speed, float divergence, boolean isFirework) {
        Vec3 initialVelocity = isFirework ? firingEntityViewVector : firingEntityViewVector.normalize();
        Vec3 velocity = initialVelocity.scale(speed);
        return new ProjectileSpawnData()
                .setVelocity(velocity)
                .setVelocityWithMaxNegativeDeviation(initialVelocity.add(-Constants.MAX_DEVIATION * divergence).scale(speed))
                .setVelocityWithMaxPositiveDeviation(initialVelocity.add(Constants.MAX_DEVIATION * divergence).scale(speed))
                .setYaw((float) (Mth.atan2(velocity.x, velocity.z) * RAD_TO_DEG))
                .setPitch((float) (Mth.atan2(velocity.y, velocity.horizontalDistance()) * RAD_TO_DEG));
    }

    /**
     * Fishing bobber has unique velocity calculation
     * @see FishingHook#tick()
     */
    public static ProjectileSpawnData calculateFishingBobberStartingVelocity(float tickProgress, Player firingEntity) {
        float entityPitch = firingEntity.getXRot(tickProgress);
        float entityYaw = firingEntity.getYRot(tickProgress);
        float h = Mth.cos(-entityYaw * DEG_TO_RAD - (float) Math.PI);
        float i = Mth.sin(-entityYaw * DEG_TO_RAD - (float) Math.PI);
        float j = -Mth.cos(-entityPitch * DEG_TO_RAD);
        float k = Mth.sin(-entityPitch * DEG_TO_RAD);
        Vec3 initialVelocity = new Vec3(-i, Mth.clamp(-(k / j), -5.0F, 5.0F), -h);
        double m = initialVelocity.length();
        Vec3 velocity = initialVelocity.scale(0.6 / m + 0.5);
        return new ProjectileSpawnData()
                .setVelocity(velocity)
                .setVelocityWithMaxNegativeDeviation(velocity.add(-Constants.FISHING_BOBBER_MAX_DEVIATION))
                .setVelocityWithMaxPositiveDeviation(velocity.add(Constants.FISHING_BOBBER_MAX_DEVIATION))
                .setYaw((float)(Mth.atan2(velocity.x, velocity.z) * RAD_TO_DEG))
                .setPitch((float)(Mth.atan2(initialVelocity.y, initialVelocity.horizontalDistance()) * RAD_TO_DEG));
    }

    /**
     * Most entities use this formula
     * @param vecY Y value of starting velocity vector
     * @param isSurfaceBubbleColumn if the block above the current bubble column block is air, then current bubble column block is a surface one
     * @param isDownward if bubble column is pointed downwards (magma)
     * @return new Y value
     * @see Entity#onInsideBubbleColumn(boolean)
     */
    public static double getBubbleVelocityUpdateForEntities(double vecY, boolean isSurfaceBubbleColumn, boolean isDownward) {
        double f;
        if (isSurfaceBubbleColumn) {
            if (isDownward) {
                f = Math.max(-0.9f, vecY + Constants.COLUMN_BUBBLE_MAGMA);
            } else {
                f = Math.min(1.8f, vecY + Constants.COLUMN_BUBBLE_SOUL_SAND_SURFACE);
            }
        } else {
            if (isDownward) {
                f = Math.max(-0.3f, vecY + Constants.COLUMN_BUBBLE_MAGMA);
            } else {
                f = Math.min(0.7f, vecY + Constants.COLUMN_BUBBLE_SOUL_SAND);
            }
        }
        return f;
    }

    /**
     * Projectile entities (except ender pearls) use this formula
     * @param isSurfaceBubbleColumn if the block above the current bubble column block is air, then current bubble column block is a surface one
     * @param isDownward if bubble column is pointed downwards (magma)
     * @return how much to add to the velocity Y value (delta)
     * @see Projectile#onInsideBubbleColumn(boolean)
     * @see ThrownEnderpearl#onInsideBubbleColumn(boolean)
     */
    public static double getBubbleDragForProjectiles(boolean isSurfaceBubbleColumn, boolean isDownward) {
        double f;
        if (isSurfaceBubbleColumn) {
            f = isDownward ? Constants.COLUMN_BUBBLE_MAGMA : Constants.COLUMN_BUBBLE_SOUL_SAND_SURFACE;
        } else {
            f = isDownward ? Constants.COLUMN_BUBBLE_MAGMA : Constants.COLUMN_BUBBLE_SOUL_SAND;
        }
        return f;
    }

    /**
     * Update entity rotation
     * @return new rotation
     * @see Projectile#lerpRotation(float, float)
     * @see AbstractArrow#tick()
     */
    public static float getNewEntityRotation(float lastRot, float newRot) {
        while (newRot - lastRot < -180.0f) {
            lastRot -= 360.0f;
        }
        while (newRot - lastRot >= 180.0f) {
            lastRot += 360.0f;
        }
        return Mth.lerp(0.2f, lastRot, newRot);
    }

    public static ProjectileData copyProjectileDataWithNewVelocityData(ProjectileData other, ProjectileSpawnData newSpawnData) {
        ProjectileData projectileData = new ProjectileData(other, -1, false);
        projectileData.setVelocityMin(Utils.copyVec3d(newSpawnData.getVelocityWithMaxNegativeDeviation()));
        projectileData.setVelocityMax(Utils.copyVec3d(newSpawnData.getVelocityWithMaxPositiveDeviation()));
        projectileData.setVelocity(Utils.copyVec3d(newSpawnData.getVelocity()));
        return projectileData;
    }
}
