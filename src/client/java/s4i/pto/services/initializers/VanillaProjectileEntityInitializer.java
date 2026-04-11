package s4i.pto.services.initializers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.Vec3d;
import s4i.pto.mixins.FireworkRocketEntityAccessor;
import s4i.pto.mixins.PersistentProjectileEntityAccessor;
import s4i.pto.model.Constants;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.model.projectile.ProjectileSpawnData;

import static s4i.pto.model.Constants.FIREWORK_OPERATION_ORDER;
import static s4i.pto.model.Constants.FISHING_BOBBER_OPERATION_ORDER;
import static s4i.pto.model.Constants.RANGED_OPERATION_ORDER;
import static s4i.pto.model.Constants.THROWABLE_OPERATION_ORDER;

public interface VanillaProjectileEntityInitializer {
   ProjectileData init(Entity projectile);

    static VanillaProjectileEntityInitializer Arrow() {
        return (projectile) -> {
            PersistentProjectileEntityAccessor arrowEntity = (PersistentProjectileEntityAccessor) projectile;
            float charge = arrowEntity.isStopped() ? 0.0f : 1.0f;
            return new ProjectileData.Builder()
                    .projectileSpawnData(new ProjectileSpawnData(projectile))
                    .gravityVelocity(new Vec3d(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                    .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                    .waterDrag(Constants.PERSISTENT_PROJECTILE_WATER_DRAG)
                    .charge(charge)
                    .operationOrder(RANGED_OPERATION_ORDER)
                    .position(projectile.getEntityPos())
                    .entity(new ArrowEntity(EntityType.ARROW, projectile.getEntityWorld()))
                    .itemClassFrom(BowItem.class)
                    .build();
        };
    }

    static VanillaProjectileEntityInitializer FireworkRocket() {
        return (projectile) -> {
            FireworkRocketEntityAccessor firework = (FireworkRocketEntityAccessor) projectile;
            int explosions = firework.getExplosiveCharges().size();
            return new ProjectileData.Builder()
                    .projectileSpawnData(new ProjectileSpawnData(projectile))
                    .gravityVelocity(new Vec3d(0, Constants.FIREWORK_GRAVITY, 0))
                    .airDrag(1.0f)
                    .waterDrag(1.0f)
                    .position(projectile.getEntityPos())
                    .operationOrder(FIREWORK_OPERATION_ORDER)
                    .itemClassFrom(CrossbowItem.class)
                    .entity(new FireworkRocketEntity(EntityType.FIREWORK_ROCKET, projectile.getEntityWorld()))
                    .forceHighlightBlock(true)
                    .ticksLeft(firework.getLifeTime() - firework.getLife())
                    .tickDeviation(0)
                    .isExplosive(true)
                    .explosions(explosions)
                    .build();
        };
    }

    static VanillaProjectileEntityInitializer Trident() {
        return (projectile) -> {
            PersistentProjectileEntityAccessor tridentEntity = (PersistentProjectileEntityAccessor) projectile;
            float charge = tridentEntity.isStopped() ? 0.0f : 1.0f;
            return new ProjectileData.Builder()
                    .projectileSpawnData(new ProjectileSpawnData(projectile))
                    .gravityVelocity(new Vec3d(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                    .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                    .waterDrag(Constants.TRIDENT_WATER_DRAG)
                    .charge(charge)
                    .position(projectile.getEntityPos())
                    .operationOrder(RANGED_OPERATION_ORDER)
                    .itemClassFrom(TridentItem.class)
                    .entity(new TridentEntity(EntityType.TRIDENT, projectile.getEntityWorld()))
                    .build();
        };
    }

    static VanillaProjectileEntityInitializer EnderPearl() {
        return ((projectile) -> {
            ProjectileData projectileData = createDefaultThrowableData(projectile);
            projectileData.setItemClassFiredFrom(EnderPearlItem.class);
            return projectileData;
        });
    }

    static VanillaProjectileEntityInitializer ExperienceBottle() {
        return (projectile) -> new ProjectileData.Builder()
                .projectileSpawnData(new ProjectileSpawnData(projectile))
                .gravityVelocity(new Vec3d(0, Constants.EXP_BOTTLE_GRAVITY, 0))
                .airDrag(Constants.THROWABLE_AIR_DRAG)
                .waterDrag(Constants.THROWABLE_WATER_DRAG)
                .position(projectile.getEntityPos())
                .operationOrder(THROWABLE_OPERATION_ORDER)
                .entity(new ExperienceBottleEntity(EntityType.EXPERIENCE_BOTTLE, projectile.getEntityWorld()))
                .itemClassFrom(ExperienceBottleItem.class)
                .build();
    }

    static VanillaProjectileEntityInitializer ThrowablePotion() {
        return (projectile) -> new ProjectileData.Builder()
                .projectileSpawnData(new ProjectileSpawnData(projectile))
                .gravityVelocity(new Vec3d(0, Constants.POTION_GRAVITY, 0))
                .airDrag(Constants.THROWABLE_AIR_DRAG)
                .waterDrag(Constants.THROWABLE_WATER_DRAG)
                .position(projectile.getEntityPos())
                .operationOrder(THROWABLE_OPERATION_ORDER)
                .entity(new SplashPotionEntity(EntityType.SPLASH_POTION, projectile.getEntityWorld()))
                .itemClassFrom(PotionItem.class)
                .build();
    }

    static VanillaProjectileEntityInitializer DefaultThrowable() {
        return VanillaProjectileEntityInitializer::createDefaultThrowableData;
    }

    private static ProjectileData createDefaultThrowableData(Entity projectile) {
        return new ProjectileData.Builder()
                .projectileSpawnData(new ProjectileSpawnData(projectile))
                .gravityVelocity(new Vec3d(0, Constants.THROWABLE_GRAVITY, 0))
                .airDrag(Constants.THROWABLE_AIR_DRAG)
                .waterDrag(Constants.THROWABLE_WATER_DRAG)
                .position(projectile.getEntityPos())
                .operationOrder(THROWABLE_OPERATION_ORDER)
                .entity(new SnowballEntity(EntityType.SNOWBALL, projectile.getEntityWorld()))
                .itemClassFrom(SnowballItem.class)
                .build();
    }

    static VanillaProjectileEntityInitializer FishingBobber() {
        return (projectile) -> new ProjectileData.Builder()
                .projectileSpawnData(new ProjectileSpawnData(projectile))
                .gravityVelocity(new Vec3d(0, Constants.THROWABLE_GRAVITY, 0))
                .airDrag(Constants.FISHING_BOBBER_DRAG)
                .waterDrag(Constants.FISHING_BOBBER_DRAG)
                .position(projectile.getEntityPos())
                .operationOrder(FISHING_BOBBER_OPERATION_ORDER)
                .entity(new FishingBobberEntity(EntityType.FISHING_BOBBER, projectile.getEntityWorld()))
                .forceHighlightBlock(true)
                .itemClassFrom(FishingRodItem.class)
                .build();
    }

    static VanillaProjectileEntityInitializer Null() {
        return (projectile -> null);
    }
}