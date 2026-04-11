package s4i.pto.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import s4i.pto.model.Constants;
import s4i.pto.model.projectile.ProjectileSpawnData;

public class ProjectileUtils {
    /**
     * Most player thrown projectiles spawn at eye level minus 0.1f
     * @param tickProgress needed to interpolate entity camera position vector between ticks
     * @param firingEntity entity doing the firing
     * @return projectile estimated spawning position as {@link Vec3d Vec3d(x,y,z)}
     */
    public static Vec3d calculateStartingPosition(float tickProgress, LivingEntity firingEntity) {
        return firingEntity.getCameraPosVec(tickProgress).add(0, -0.1f, 0);
    }

    /**
     * Crossbows spawn their projectiles at eye level minus 0.15f
     * @see CrossbowItem#createArrowEntity
     */
    public static Vec3d calculateCrossbowStartingPosition(float tickProgress, LivingEntity firingEntity) {
        return firingEntity.getCameraPosVec(tickProgress).add(0, -0.15f, 0);
    }

    /**
     * Fishing bobbers take entity's yaw into consideration
     * @see FishingBobberEntity#FishingBobberEntity(PlayerEntity, World, int, int)
     */
    public static Vec3d calculateFishingBobberStartingPosition(float tickProgress, LivingEntity firingEntity) {
        float g = firingEntity.getYaw(tickProgress);
        float h = MathHelper.cos(-g * (float) (Math.PI / 180.0) - (float) Math.PI);
        float i = MathHelper.sin(-g * (float) (Math.PI / 180.0) - (float) Math.PI);
        return firingEntity.getCameraPosVec(tickProgress).add(-i * 0.3, -0.1f, -h * 0.3);
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
     * @param divergence randomized spread used in {@link ProjectileEntity#calculateVelocity}, for players it is set to 1.0f
     * @param roll certain thrown items like {@link net.minecraft.item.ThrowablePotionItem#use} use this and multi-shot crossbows
     * @return simulated projectile data to be used for estimations
     */
    public static ProjectileSpawnData calculateRangedWeaponItemStartingVelocity(float tickProgress, PlayerEntity firingEntity, float speed, float divergence, float roll) {
        float yaw = firingEntity.getYaw(tickProgress);
        float pitch = firingEntity.getPitch(tickProgress);
        float f = -MathHelper.sin(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
        float g = -MathHelper.sin((pitch + roll) * (float) (Math.PI / 180.0));
        float h = MathHelper.cos(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
        Vec3d initialVelocity = new Vec3d(f, g, h);
        Vec3d velocity = initialVelocity.normalize().multiply(speed);
        Vec3d initialFiringEntityVelocity = EntityUtils.getEntityMovement(firingEntity);
        Vec3d finalFiringEntityVelocity = firingEntity.isOnGround() || firingEntity.hasVehicle() ?
                initialFiringEntityVelocity.multiply(1, 0, 1) : initialFiringEntityVelocity;
        Vec3d finalVelocity = velocity.add(finalFiringEntityVelocity);
        return new ProjectileSpawnData()
                .setVelocity(finalVelocity)
                .setVelocityWithMaxNegativeDeviation(initialVelocity.normalize().add(-Constants.MAX_DEVIATION * divergence).multiply(speed).add(finalFiringEntityVelocity))
                .setVelocityWithMaxPositiveDeviation(initialVelocity.normalize().add(Constants.MAX_DEVIATION * divergence).multiply(speed).add(finalFiringEntityVelocity))
                .setYaw((float) (MathHelper.atan2(velocity.x, velocity.z) * 180.0F / (float) Math.PI))
                .setPitch((float) (MathHelper.atan2(velocity.y, velocity.horizontalLength()) * 180.0F / (float) Math.PI));
    }

    /**
     * Crossbows do not take player or vehicle movement into consideration
     * @see CrossbowItem#shoot
     */
    public static ProjectileSpawnData calculateCrossbowStartingVelocity(float tickProgress, PlayerEntity firingEntity, float speed, float divergence,
                                                                        boolean isFirework) {
        Vec3d cameraRotationVector = firingEntity.getRotationVec(tickProgress);
        Vec3d initialVelocity = isFirework ? cameraRotationVector : cameraRotationVector.normalize();
        Vec3d velocity = initialVelocity.multiply(speed);
        return new ProjectileSpawnData()
                .setVelocity(velocity)
                .setVelocityWithMaxNegativeDeviation(initialVelocity.add(-Constants.MAX_DEVIATION * divergence).multiply(speed))
                .setVelocityWithMaxPositiveDeviation(initialVelocity.add(Constants.MAX_DEVIATION * divergence).multiply(speed))
                .setYaw((float) (MathHelper.atan2(velocity.x, velocity.z) * 180.0F / (float) Math.PI))
                .setPitch((float) (MathHelper.atan2(velocity.y, velocity.horizontalLength()) * 180.0F / (float) Math.PI));
    }

    /**
     * Fishing bobber has unique velocity calculation
     * @see FishingBobberEntity#tick()
     */
    public static ProjectileSpawnData calculateFishingBobberStartingVelocity(float tickProgress, PlayerEntity firingEntity) {
        float entityPitch = firingEntity.getPitch(tickProgress);
        float entityYaw = firingEntity.getYaw(tickProgress);
        float h = MathHelper.cos(-entityYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float i = MathHelper.sin(-entityYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float j = -MathHelper.cos(-entityPitch * (float) (Math.PI / 180.0));
        float k = MathHelper.sin(-entityPitch * (float) (Math.PI / 180.0));
        Vec3d initialVelocity = new Vec3d(-i, MathHelper.clamp(-(k / j), -5.0F, 5.0F), -h);
        double m = initialVelocity.length();
        Vec3d velocity = initialVelocity.multiply(0.6 / m + 0.5);
        return new ProjectileSpawnData()
                .setVelocity(velocity)
                .setVelocityWithMaxNegativeDeviation(velocity.add(-Constants.FISHING_BOBBER_MAX_DEVIATION))
                .setVelocityWithMaxPositiveDeviation(velocity.add(Constants.FISHING_BOBBER_MAX_DEVIATION))
                .setYaw((float)(MathHelper.atan2(velocity.x, velocity.z) * 180.0F / (float)Math.PI))
                .setPitch((float)(MathHelper.atan2(initialVelocity.y, initialVelocity.horizontalLength()) * 180.0F / (float)Math.PI));
    }

    /**
     * Most entities use this formula
     * @param vecY Y value of starting velocity vector
     * @param isSurfaceBubbleColumn if the block above the current bubble column block is air, then current bubble column block is a surface one
     * @param isDownward if bubble column is pointed downwards (magma)
     * @return new Y value
     * @see Entity#onBubbleColumnCollision
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
     * @see ProjectileEntity#onBubbleColumnCollision 
     * @see net.minecraft.entity.projectile.thrown.EnderPearlEntity#onBubbleColumnCollision 
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
     * @see ProjectileEntity#updateRotation(float, float)
     * @see PersistentProjectileEntity#tick()
     */
    public static float getNewEntityRotation(float lastRot, float newRot) {
        while (newRot - lastRot < -180.0f) {
            lastRot -= 360.0f;
        }
        while (newRot - lastRot >= 180.0f) {
            lastRot += 360.0f;
        }
        return MathHelper.lerp(0.2f, lastRot, newRot);
    }
}
