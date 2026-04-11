package s4i.pto.services.initializers;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import s4i.pto.model.Constants;
import s4i.pto.model.projectile.ItemData;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.utils.ProjectileUtils;

import java.util.ArrayList;
import java.util.List;

import static s4i.pto.model.Constants.FIREWORK_OPERATION_ORDER;
import static s4i.pto.model.Constants.FISHING_BOBBER_OPERATION_ORDER;
import static s4i.pto.model.Constants.RANGED_OPERATION_ORDER;
import static s4i.pto.model.Constants.THROWABLE_OPERATION_ORDER;

public interface VanillaProjectileItemInitializer {
    List<ProjectileData> init(float tickProgress, PlayerEntity firingEntity, ItemData itemData);

    static VanillaProjectileItemInitializer Bow() {
        return (tickProgress, firingEntity, itemData) -> {
            float power = Constants.BOW_ARROW_MAX_SPEED * BowItem.getPullProgress(firingEntity.getItemUseTime());
            return List.of(new ProjectileData.Builder()
                    .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                            tickProgress, firingEntity, power, Constants.DEFAULT_PLAYER_DEVIATION, 0f))
                    .gravityVelocity(new Vec3d(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                    .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                    .waterDrag(Constants.PERSISTENT_PROJECTILE_WATER_DRAG)
                    .charge(ProjectileUtils.toQuadraticScale(power, Constants.BOW_ARROW_MAX_SPEED))
                    .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                    .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                    .operationOrder(RANGED_OPERATION_ORDER)
                    .entity(new ArrowEntity(EntityType.ARROW, firingEntity.getEntityWorld()))
                    .itemClassFrom(itemData.clas)
                    .build());
        };
    }

    static VanillaProjectileItemInitializer Crossbow() {
        return (tickProgress, firingEntity, itemData) -> {
            int multishotEnchantmentLevel = 0;
            int piercingEnchantmentLevel = 0;
            for (RegistryEntry<Enchantment> enchantment : EnchantmentHelper.getEnchantments(itemData.stack).getEnchantments()) {
                if (enchantment.getIdAsString().equals(Enchantments.MULTISHOT.getValue().toString())) {
                    multishotEnchantmentLevel = EnchantmentHelper.getLevel(enchantment,itemData.stack);
                } else if (enchantment.getIdAsString().equals(Enchantments.PIERCING.getValue().toString())) {
                    piercingEnchantmentLevel = EnchantmentHelper.getLevel(enchantment, itemData.stack);
                }
            }

            ChargedProjectilesComponent loadedProjectiles = itemData.stack.get(DataComponentTypes.CHARGED_PROJECTILES);
            if (loadedProjectiles == null || loadedProjectiles.isEmpty()) {
                return null;
            }
            float charge = CrossbowItem.isCharged(itemData.stack) ? 1.0f : 0.0f;

            List<ProjectileData> projectileDataList = new ArrayList<>();
            ProjectileData centerProjectile;
            if (loadedProjectiles.contains(Items.FIREWORK_ROCKET)) {
                FireworksComponent fireworksComponent = loadedProjectiles.getProjectiles().getFirst().get(DataComponentTypes.FIREWORKS);
                int explosions = fireworksComponent.explosions().size();
                int lifeTime = 10 * (1 + fireworksComponent.flightDuration()) + Constants.FIREWORK_ROCKET_MAX_LIFETIME_DEVIATION / 2;
                centerProjectile = new ProjectileData.Builder()
                        .projectileSpawnData(ProjectileUtils.calculateCrossbowStartingVelocity(
                                tickProgress, firingEntity, Constants.CROSSBOW_FIREWORK_MAX_SPEED, Constants.DEFAULT_PLAYER_DEVIATION, true))
                        .gravityVelocity(new Vec3d(0, Constants.FIREWORK_GRAVITY, 0))
                        .airDrag(1.0f)
                        .waterDrag(1.0f)
                        .charge(charge)
                        .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                        .position(ProjectileUtils.calculateCrossbowStartingPosition(tickProgress, firingEntity))
                        .operationOrder(FIREWORK_OPERATION_ORDER)
                        .itemClassFrom(itemData.clas)
                        .entity(new FireworkRocketEntity(EntityType.FIREWORK_ROCKET, firingEntity.getEntityWorld()))
                        .forceHighlightBlock(true)
                        .ticksLeft(lifeTime)
                        .tickDeviation(Constants.FIREWORK_ROCKET_MAX_LIFETIME_DEVIATION / 2)
                        .isExplosive(true)
                        .explosions(explosions)
                        .build();
            } else if (loadedProjectiles.contains(Items.ARROW)) {
                centerProjectile = new ProjectileData.Builder()
                        .projectileSpawnData(ProjectileUtils.calculateCrossbowStartingVelocity(
                                tickProgress, firingEntity, Constants.CROSSBOW_ARROW_MAX_SPEED, Constants.DEFAULT_PLAYER_DEVIATION, false))
                        .gravityVelocity(new Vec3d(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                        .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                        .waterDrag(Constants.PERSISTENT_PROJECTILE_WATER_DRAG)
                        .charge(charge)
                        .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                        .position(ProjectileUtils.calculateCrossbowStartingPosition(tickProgress, firingEntity))
                        .operationOrder(RANGED_OPERATION_ORDER)
                        .itemClassFrom(itemData.clas)
                        .entity(new ArrowEntity(EntityType.ARROW, firingEntity.getEntityWorld()))
                        .pierces(piercingEnchantmentLevel)
                        .build();
            } else {
                return null;
            }
            projectileDataList.add(centerProjectile);
            for (int i = 0; i < multishotEnchantmentLevel; i++) {
                ProjectileData left = new ProjectileData(centerProjectile, -1, false);
                left.setVelocity(left.getVelocity().rotateY((float) Math.toRadians((i + 1) * 10f)));
                ProjectileData right = new ProjectileData(centerProjectile, -1, false);
                right.setVelocity(right.getVelocity().rotateY((float) Math.toRadians((i + 1) * -10f)));
                projectileDataList.add(left);
                projectileDataList.add(right);
            }
            return projectileDataList;
        };
    }

    static VanillaProjectileItemInitializer Trident() {
        return (tickProgress, firingEntity, itemData) -> {
            int riptideLevel = 0;
            for (RegistryEntry<Enchantment> enchantment : EnchantmentHelper.getEnchantments(itemData.stack).getEnchantments()) {
                if (enchantment.getIdAsString().equals(Enchantments.RIPTIDE.getValue().toString())) {
                    riptideLevel = EnchantmentHelper.getLevel(enchantment, itemData.stack);
                }
            }
            float charge = firingEntity.getItemUseTime() < TridentItem.MIN_DRAW_DURATION ? 0f : 1.0f;
            if (riptideLevel <= 0) {
                float power = TridentItem.THROW_SPEED;
                return List.of(new ProjectileData.Builder()
                        .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                                tickProgress, firingEntity, power, Constants.DEFAULT_PLAYER_DEVIATION, 0f))
                        .gravityVelocity(new Vec3d(0, Constants.PERSISTENT_PROJECTILE_GRAVITY, 0))
                        .airDrag(Constants.PERSISTENT_PROJECTILE_AIR_DRAG)
                        .waterDrag(Constants.TRIDENT_WATER_DRAG)
                        .charge(charge)
                        .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                        .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                        .operationOrder(RANGED_OPERATION_ORDER)
                        .itemClassFrom(itemData.clas)
                        .entity(new TridentEntity(EntityType.TRIDENT, firingEntity.getEntityWorld()))
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
            projectileData.getFirst().setItemClassFiredFrom(EnderPearlItem.class);
            return projectileData;
        });
    }

    static VanillaProjectileItemInitializer ExperienceBottle() {
        return (tickProgress, firingEntity, itemData) -> {
            float power = Constants.EXP_BOTTLE_SPEED;
            return List.of(new ProjectileData.Builder()
                    .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                            tickProgress, firingEntity, power, Constants.DEFAULT_PLAYER_DEVIATION, -20f))
                    .gravityVelocity(new Vec3d(0, Constants.EXP_BOTTLE_GRAVITY, 0))
                    .airDrag(Constants.THROWABLE_AIR_DRAG)
                    .waterDrag(Constants.THROWABLE_WATER_DRAG)
                    .charge(1.0f)
                    .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                    .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                    .operationOrder(THROWABLE_OPERATION_ORDER)
                    .entity(new ExperienceBottleEntity(EntityType.EXPERIENCE_BOTTLE, firingEntity.getEntityWorld()))
                    .itemClassFrom(itemData.clas)
                    .build());
        };
    }

    static VanillaProjectileItemInitializer ThrowablePotion() {
        return (tickProgress, firingEntity, itemData) -> {
            float power = Constants.POTION_BOTTLE_SPEED;
            return List.of(new ProjectileData.Builder()
                    .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                            tickProgress, firingEntity, power, Constants.DEFAULT_PLAYER_DEVIATION, -20f))
                    .gravityVelocity(new Vec3d(0, Constants.POTION_GRAVITY, 0))
                    .airDrag(Constants.THROWABLE_AIR_DRAG)
                    .waterDrag(Constants.THROWABLE_WATER_DRAG)
                    .charge(1.0f)
                    .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                    .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                    .operationOrder(THROWABLE_OPERATION_ORDER)
                    .entity(new SplashPotionEntity(EntityType.SPLASH_POTION, firingEntity.getEntityWorld()))
                    .itemClassFrom(itemData.clas)
                    .build());
        };
    }

    static VanillaProjectileItemInitializer Snowball() {
        return VanillaProjectileItemInitializer::createDefaultThrowableData;
    }

    private static List<ProjectileData> createDefaultThrowableData(float tickProgress, PlayerEntity firingEntity, ItemData itemData) {
        return List.of(new ProjectileData.Builder()
                .projectileSpawnData(ProjectileUtils.calculateRangedWeaponItemStartingVelocity(
                        tickProgress, firingEntity, Constants.DEFAULT_THROWABLE_SPEED, Constants.DEFAULT_PLAYER_DEVIATION, 0f))
                .gravityVelocity(new Vec3d(0, Constants.THROWABLE_GRAVITY, 0))
                .airDrag(Constants.THROWABLE_AIR_DRAG)
                .waterDrag(Constants.THROWABLE_WATER_DRAG)
                .charge(1.0f)
                .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                .position(ProjectileUtils.calculateStartingPosition(tickProgress, firingEntity))
                .operationOrder(THROWABLE_OPERATION_ORDER)
                .entity(new SnowballEntity(EntityType.SNOWBALL, firingEntity.getEntityWorld()))
                .itemClassFrom(itemData.clas)
                .build());
    }

    static VanillaProjectileItemInitializer FishingRod() {
        return (tickProgress, firingEntity, itemData) -> {
            float charge = firingEntity.fishHook != null ? 0.0f : 1.0f;
            return List.of(new ProjectileData.Builder()
                    .projectileSpawnData(ProjectileUtils.calculateFishingBobberStartingVelocity(tickProgress, firingEntity))
                    .gravityVelocity(new Vec3d(0, Constants.THROWABLE_GRAVITY, 0))
                    .airDrag(Constants.FISHING_BOBBER_DRAG)
                    .waterDrag(Constants.FISHING_BOBBER_DRAG)
                    .charge(charge)
                    .inaccuracy(Constants.DEFAULT_PLAYER_DEVIATION)
                    .position(ProjectileUtils.calculateFishingBobberStartingPosition(tickProgress, firingEntity))
                    .operationOrder(FISHING_BOBBER_OPERATION_ORDER)
                    .entity(new FishingBobberEntity(EntityType.FISHING_BOBBER, firingEntity.getEntityWorld()))
                    .forceHighlightBlock(true)
                    .itemClassFrom(itemData.clas)
                    .build());
        };
    }

    static VanillaProjectileItemInitializer Null() {
        return (tickProgress, firingEntity, itemData) -> null;
    }
}