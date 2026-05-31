package s4i.pto.services.initializers;


import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;
import s4i.pto.mixins.AbstractArrowAccessor;
import s4i.pto.mixins.FireworkRocketEntityAccessor;
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
            AbstractArrowAccessor arrowEntity = (AbstractArrowAccessor) projectile;
            float charge = arrowEntity.isStopped() ? 0.0f : 1.0f;
            return ProjectileData.builder()
                    .projectileSpawnData(new ProjectileSpawnData(projectile))
                    .gravityVelocity(new Vec3(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                    .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                    .waterDrag(Constants.PERSISTENT_PROJECTILE_WATER_DRAG)
                    .charge(charge)
                    .operationOrder(RANGED_OPERATION_ORDER)
                    .position(projectile.position())
                    .entity(new Arrow(EntityType.ARROW, projectile.level()))
                    .itemClassFiredFrom(BowItem.class)
                    .build();
        };
    }

    static VanillaProjectileEntityInitializer FireworkRocket() {
        return (projectile) -> {
            FireworkRocketEntityAccessor firework = (FireworkRocketEntityAccessor) projectile;
            int explosions = firework.getExplosiveCharges().size();
            return ProjectileData.builder()
                    .projectileSpawnData(new ProjectileSpawnData(projectile))
                    .gravityVelocity(new Vec3(0, Constants.FIREWORK_GRAVITY, 0))
                    .airDrag(1.0f)
                    .waterDrag(1.0f)
                    .position(projectile.position())
                    .operationOrder(FIREWORK_OPERATION_ORDER)
                    .itemClassFiredFrom(CrossbowItem.class)
                    .entity(new FireworkRocketEntity(EntityType.FIREWORK_ROCKET, projectile.level()))
                    .forceHighlightBlock(true)
                    .ticksLeft(firework.getLifetime() - firework.getLife())
                    .tickDeviation(0)
                    .isExplosive(true)
                    .explosions(explosions)
                    .build();
        };
    }

    static VanillaProjectileEntityInitializer Trident() {
        return (projectile) -> {
            AbstractArrowAccessor tridentEntity = (AbstractArrowAccessor) projectile;
            float charge = tridentEntity.isStopped() ? 0.0f : 1.0f;
            return ProjectileData.builder()
                    .projectileSpawnData(new ProjectileSpawnData(projectile))
                    .gravityVelocity(new Vec3(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                    .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                    .waterDrag(Constants.TRIDENT_WATER_DRAG)
                    .charge(charge)
                    .position(projectile.position())
                    .operationOrder(RANGED_OPERATION_ORDER)
                    .itemClassFiredFrom(TridentItem.class)
                    .entity(new ThrownTrident(EntityType.TRIDENT, projectile.level()))
                    .build();
        };
    }

    static VanillaProjectileEntityInitializer EnderPearl() {
        return ((projectile) -> {
            ProjectileData projectileData = createDefaultThrowableData(projectile);
            projectileData.setItemClassFiredFrom(EnderpearlItem.class);
            return projectileData;
        });
    }

    static VanillaProjectileEntityInitializer ExperienceBottle() {
        return (projectile) -> ProjectileData.builder()
                .projectileSpawnData(new ProjectileSpawnData(projectile))
                .gravityVelocity(new Vec3(0, Constants.EXP_BOTTLE_GRAVITY, 0))
                .airDrag(Constants.THROWABLE_AIR_DRAG)
                .waterDrag(Constants.THROWABLE_WATER_DRAG)
                .position(projectile.position())
                .operationOrder(THROWABLE_OPERATION_ORDER)
                .entity(new ThrownExperienceBottle(EntityType.EXPERIENCE_BOTTLE, projectile.level()))
                .itemClassFiredFrom(ExperienceBottleItem.class)
                .build();
    }

    static VanillaProjectileEntityInitializer ThrowablePotion() {
        return (projectile) -> ProjectileData.builder()
                .projectileSpawnData(new ProjectileSpawnData(projectile))
                .gravityVelocity(new Vec3(0, Constants.POTION_GRAVITY, 0))
                .airDrag(Constants.THROWABLE_AIR_DRAG)
                .waterDrag(Constants.THROWABLE_WATER_DRAG)
                .position(projectile.position())
                .operationOrder(THROWABLE_OPERATION_ORDER)
                .entity(new ThrownSplashPotion(EntityType.SPLASH_POTION, projectile.level()))
                .itemClassFiredFrom(PotionItem.class)
                .build();
    }

    static VanillaProjectileEntityInitializer DefaultThrowable() {
        return VanillaProjectileEntityInitializer::createDefaultThrowableData;
    }

    private static ProjectileData createDefaultThrowableData(Entity projectile) {
        return ProjectileData.builder()
                .projectileSpawnData(new ProjectileSpawnData(projectile))
                .gravityVelocity(new Vec3(0, Constants.THROWABLE_GRAVITY, 0))
                .airDrag(Constants.THROWABLE_AIR_DRAG)
                .waterDrag(Constants.THROWABLE_WATER_DRAG)
                .position(projectile.position())
                .operationOrder(THROWABLE_OPERATION_ORDER)
                .entity(new Snowball(EntityType.SNOWBALL, projectile.level()))
                .itemClassFiredFrom(SnowballItem.class)
                .build();
    }

    static VanillaProjectileEntityInitializer FishingBobber() {
        return (projectile) -> ProjectileData.builder()
                .projectileSpawnData(new ProjectileSpawnData(projectile))
                .gravityVelocity(new Vec3(0, Constants.THROWABLE_GRAVITY, 0))
                .airDrag(Constants.FISHING_BOBBER_DRAG)
                .waterDrag(Constants.FISHING_BOBBER_DRAG)
                .position(projectile.position())
                .operationOrder(FISHING_BOBBER_OPERATION_ORDER)
                .entity(new FishingHook(EntityType.FISHING_BOBBER, projectile.level()))
                .forceHighlightBlock(true)
                .itemClassFiredFrom(FishingRodItem.class)
                .build();
    }

    static VanillaProjectileEntityInitializer Null() {
        return (_ -> null);
    }
}