package s4i.pto.services.initializers;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import s4i.pto.model.Constants;
import s4i.pto.model.projectile.ItemData;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.model.projectile.ProjectileSpawnData;
import s4i.pto.utils.ProjectileUtils;

import java.util.ArrayList;
import java.util.List;

import static s4i.pto.model.Constants.DEG_TO_RAD;
import static s4i.pto.model.Constants.FIREWORK_OPERATION_ORDER;
import static s4i.pto.model.Constants.FISHING_BOBBER_OPERATION_ORDER;
import static s4i.pto.model.Constants.RANGED_OPERATION_ORDER;
import static s4i.pto.model.Constants.THROWABLE_OPERATION_ORDER;

public interface VanillaProjectileItemInitializer {
    List<ProjectileData> init(float tickProgress, Player firingEntity, ItemData itemData);

    static VanillaProjectileItemInitializer Bow() {
        return (tickProgress, firingEntity, itemData) -> {
            float power = Constants.BOW_ARROW_MAX_SPEED * BowItem.getPowerForTime(firingEntity.getTicksUsingItem());
            return List.of(ProjectileData.builder()
                    .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                            tickProgress, firingEntity, power, Constants.DEFAULT_PLAYER_DEVIATION, 0f))
                    .gravityVelocity(new Vec3(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                    .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                    .waterDrag(Constants.PERSISTENT_PROJECTILE_WATER_DRAG)
                    .charge(ProjectileUtils.toQuadraticScale(power, Constants.BOW_ARROW_MAX_SPEED))
                    .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                    .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                    .operationOrder(RANGED_OPERATION_ORDER)
                    .entity(new Arrow(EntityType.ARROW, firingEntity.level()))
                    .itemClassFiredFrom(itemData.getItemClass())
                    .build());
        };
    }

    static VanillaProjectileItemInitializer Crossbow() {
        return (tickProgress, firingEntity, itemData) -> {
            ChargedProjectiles loadedProjectiles = itemData.getStack().get(DataComponents.CHARGED_PROJECTILES);
            if (loadedProjectiles == null) {
                return null;
            }

            RegistryAccess registries = firingEntity.level().registryAccess();
            Holder<Enchantment> multishotEnchantment = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT);
            Holder<Enchantment> piercingEnchantment = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PIERCING);
            int multishotEnchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(multishotEnchantment, itemData.getStack());
            int piercingEnchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(piercingEnchantment, itemData.getStack());

            float charge = CrossbowItem.isCharged(itemData.getStack()) ? 1.0f : 0.0f;

            List<ProjectileData> projectileDataList = new ArrayList<>();
            ProjectileData centerProjectile;
            boolean isFirework = false;
            if (loadedProjectiles.contains(Items.FIREWORK_ROCKET)) {
                isFirework = true;
                ItemStack projectile = loadedProjectiles.itemCopies().getFirst();
                Fireworks fireworkComponent = projectile.get(DataComponents.FIREWORKS);
                int explosions = fireworkComponent != null ? fireworkComponent.explosions().size() : 0;
                int lifeTime = fireworkComponent != null ?
                        10 * (1 + fireworkComponent.flightDuration()) + Constants.FIREWORK_ROCKET_MAX_LIFETIME_DEVIATION / 2 : 0;

                centerProjectile = ProjectileData.builder()
                        .projectileSpawnData(ProjectileUtils.calculateCrossbowStartingVelocity(firingEntity.getViewVector(tickProgress),
                                Constants.CROSSBOW_FIREWORK_MAX_SPEED, Constants.DEFAULT_PLAYER_DEVIATION, true))
                        .gravityVelocity(new Vec3(0, Constants.FIREWORK_GRAVITY, 0))
                        .airDrag(1.0f)
                        .waterDrag(1.0f)
                        .charge(charge)
                        .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                        .position(ProjectileUtils.calculateCrossbowStartingPosition(tickProgress, firingEntity))
                        .operationOrder(FIREWORK_OPERATION_ORDER)
                        .itemClassFiredFrom(itemData.getItemClass())
                        .entity(new FireworkRocketEntity(EntityType.FIREWORK_ROCKET, firingEntity.level()))
                        .forceHighlightBlock(true)
                        .ticksLeft(lifeTime)
                        .tickDeviation(Constants.FIREWORK_ROCKET_MAX_LIFETIME_DEVIATION / 2)
                        .isExplosive(explosions > 0)
                        .explosions(explosions)
                        .build();
            } else if (loadedProjectiles.contains(Items.ARROW)) {
                centerProjectile = ProjectileData.builder()
                        .projectileSpawnData(ProjectileUtils.calculateCrossbowStartingVelocity(firingEntity.getViewVector(tickProgress),
                                Constants.CROSSBOW_ARROW_MAX_SPEED, Constants.DEFAULT_PLAYER_DEVIATION, false))
                        .gravityVelocity(new Vec3(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                        .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                        .waterDrag(Constants.PERSISTENT_PROJECTILE_WATER_DRAG)
                        .charge(charge)
                        .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                        .position(ProjectileUtils.calculateCrossbowStartingPosition(tickProgress, firingEntity))
                        .operationOrder(RANGED_OPERATION_ORDER)
                        .itemClassFiredFrom(itemData.getItemClass())
                        .entity(new Arrow(EntityType.ARROW, firingEntity.level()))
                        .pierces(piercingEnchantmentLevel)
                        .build();
            } else {
                return null;
            }
            projectileDataList.add(centerProjectile);
            for (int i = 0; i < multishotEnchantmentLevel; i++) {
                float speed = isFirework ? Constants.CROSSBOW_FIREWORK_MAX_SPEED : Constants.CROSSBOW_ARROW_MAX_SPEED;
                float angle = (i + 1) * 10f;
                Vec3 upVector = firingEntity.getUpVector(1.0f);
                Quaternionf upQuaternionLeft = new Quaternionf().setAngleAxis(angle * DEG_TO_RAD, upVector.x, upVector.y, upVector.z);
                Quaternionf upQuaternionRight = new Quaternionf().setAngleAxis(-angle * DEG_TO_RAD, upVector.x, upVector.y, upVector.z);
                Vec3 newViewVectorLeft = new Vec3(firingEntity.getViewVector(1.0f).toVector3f().rotate(upQuaternionLeft));
                Vec3 newViewVectorRight = new Vec3(firingEntity.getViewVector(1.0f).toVector3f().rotate(upQuaternionRight));
                ProjectileSpawnData leftData = ProjectileUtils.calculateCrossbowStartingVelocity(newViewVectorLeft, speed,
                        Constants.DEFAULT_PLAYER_DEVIATION, isFirework);
                ProjectileSpawnData rightData = ProjectileUtils.calculateCrossbowStartingVelocity(newViewVectorRight, speed,
                        Constants.DEFAULT_PLAYER_DEVIATION, isFirework);
                ProjectileData left = ProjectileUtils.copyProjectileDataWithNewVelocityData(centerProjectile, leftData);
                ProjectileData right = ProjectileUtils.copyProjectileDataWithNewVelocityData(centerProjectile, rightData);
                projectileDataList.add(left);
                projectileDataList.add(right);
            }
            return projectileDataList;
        };
    }

    static VanillaProjectileItemInitializer Trident() {
        return (tickProgress, firingEntity, itemData) -> {
            RegistryAccess registries = firingEntity.level().registryAccess();
            Holder<Enchantment> riptideEnchantment = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.RIPTIDE);
            int riptideLevel = EnchantmentHelper.getItemEnchantmentLevel(riptideEnchantment, itemData.getStack());

            float charge = firingEntity.getTicksUsingItem() < TridentItem.THROW_THRESHOLD_TIME ? 0f : 1.0f;
            if (riptideLevel <= 0) {
                float power = TridentItem.PROJECTILE_SHOOT_POWER;
                return List.of(ProjectileData.builder()
                        .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                                tickProgress, firingEntity, power, Constants.DEFAULT_PLAYER_DEVIATION, 0f))
                        .gravityVelocity(new Vec3(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                        .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                        .waterDrag(Constants.TRIDENT_WATER_DRAG)
                        .charge(charge)
                        .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                        .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                        .operationOrder(RANGED_OPERATION_ORDER)
                        .itemClassFiredFrom(itemData.getItemClass())
                        .entity(new ThrownTrident(EntityType.TRIDENT, firingEntity.level()))
                        .build());
            } else {
                return null;
            }
        };
    }

    static VanillaProjectileItemInitializer Egg() {
        return VanillaProjectileItemInitializer::createDefaultThrowableData;
    }

    static VanillaProjectileItemInitializer EnderPearl() {
        return ((tickProgress, firingEntity, itemData) -> {
            List<ProjectileData> projectileData = createDefaultThrowableData(tickProgress, firingEntity, itemData);
            projectileData.getFirst().setItemClassFiredFrom(EnderpearlItem.class);
            return projectileData;
        });
    }

    static VanillaProjectileItemInitializer ExperienceBottle() {
        return (tickProgress, firingEntity, itemData) -> {
            float power = Constants.EXP_BOTTLE_SPEED;
            return List.of(ProjectileData.builder()
                    .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                            tickProgress, firingEntity, power, Constants.DEFAULT_PLAYER_DEVIATION, -20f))
                    .gravityVelocity(new Vec3(0, Constants.EXP_BOTTLE_GRAVITY, 0))
                    .airDrag(Constants.THROWABLE_AIR_DRAG)
                    .waterDrag(Constants.THROWABLE_WATER_DRAG)
                    .charge(1.0f)
                    .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                    .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                    .operationOrder(THROWABLE_OPERATION_ORDER)
                    .entity(new ThrownExperienceBottle(EntityType.EXPERIENCE_BOTTLE, firingEntity.level()))
                    .itemClassFiredFrom(itemData.getItemClass())
                    .build());
        };
    }

    static VanillaProjectileItemInitializer ThrowablePotion() {
        return (tickProgress, firingEntity, itemData) -> {
            float power = Constants.POTION_BOTTLE_SPEED;
            return List.of(ProjectileData.builder()
                    .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                            tickProgress, firingEntity, power, Constants.DEFAULT_PLAYER_DEVIATION, -20f))
                    .gravityVelocity(new Vec3(0, Constants.POTION_GRAVITY, 0))
                    .airDrag(Constants.THROWABLE_AIR_DRAG)
                    .waterDrag(Constants.THROWABLE_WATER_DRAG)
                    .charge(1.0f)
                    .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                    .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                    .operationOrder(THROWABLE_OPERATION_ORDER)
                    .entity(new ThrownSplashPotion(EntityType.SPLASH_POTION, firingEntity.level()))
                    .itemClassFiredFrom(itemData.getItemClass())
                    .build());
        };
    }

    static VanillaProjectileItemInitializer Snowball() {
        return VanillaProjectileItemInitializer::createDefaultThrowableData;
    }

    private static List<ProjectileData> createDefaultThrowableData(float tickProgress, Player firingEntity, ItemData itemData) {
        return List.of(ProjectileData.builder()
                .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                        tickProgress, firingEntity, Constants.DEFAULT_THROWABLE_SPEED, Constants.DEFAULT_PLAYER_DEVIATION, 0f))
                .gravityVelocity(new Vec3(0, Constants.THROWABLE_GRAVITY, 0))
                .airDrag(Constants.THROWABLE_AIR_DRAG)
                .waterDrag(Constants.THROWABLE_WATER_DRAG)
                .charge(1.0f)
                .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                .operationOrder(THROWABLE_OPERATION_ORDER)
                .entity(new Snowball(EntityType.SNOWBALL, firingEntity.level()))
                .itemClassFiredFrom(itemData.getItemClass())
                .build());
    }

    static VanillaProjectileItemInitializer FishingRod() {
        return (tickProgress, firingEntity, itemData) -> {
            float charge = firingEntity.fishing != null ? 0.0f : 1.0f;
            return List.of(ProjectileData.builder()
                    .projectileSpawnData(ProjectileUtils.calculateFishingBobberStartingVelocity(tickProgress, firingEntity))
                    .gravityVelocity(new Vec3(0, Constants.THROWABLE_GRAVITY, 0))
                    .airDrag(Constants.FISHING_BOBBER_DRAG)
                    .waterDrag(Constants.FISHING_BOBBER_DRAG)
                    .charge(charge)
                    .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                    .position(ProjectileUtils.calculateFishingBobberStartingPosition(tickProgress, firingEntity))
                    .operationOrder(FISHING_BOBBER_OPERATION_ORDER)
                    .entity(new FishingHook(EntityType.FISHING_BOBBER, firingEntity.level()))
                    .forceHighlightBlock(true)
                    .itemClassFiredFrom(itemData.getItemClass())
                    .build());
        };
    }

    static VanillaProjectileItemInitializer Null() {
        return (_, _, _) -> null;
    }
}