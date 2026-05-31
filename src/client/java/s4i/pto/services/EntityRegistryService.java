package s4i.pto.services;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.services.initializers.ModdedProjectileEntityInitializer;
import s4i.pto.services.initializers.VanillaProjectileEntityInitializer;

import java.util.HashMap;
import java.util.Map;

public class EntityRegistryService {
    private static EntityRegistryService instance;

    private Map<Class<?>, VanillaProjectileEntityInitializer> vanillaDataMap;
    private Map<Class<?>, ModdedProjectileEntityInitializer> moddedDataMap;

    private EntityRegistryService() {}

    public static EntityRegistryService getInstance() {
        if (instance == null) {
            instance = new EntityRegistryService();
            instance.init();
        }
        return instance;
    }

    private void init() {
        vanillaDataMap = new HashMap<>(20);
        vanillaDataMap.put(Arrow.class, VanillaProjectileEntityInitializer.Arrow());
        vanillaDataMap.put(ThrownTrident.class, VanillaProjectileEntityInitializer.Trident());
        vanillaDataMap.put(ThrownEgg.class, VanillaProjectileEntityInitializer.DefaultThrowable());
        vanillaDataMap.put(Snowball.class, VanillaProjectileEntityInitializer.DefaultThrowable());
        vanillaDataMap.put(ThrownEnderpearl.class, VanillaProjectileEntityInitializer.EnderPearl());
        vanillaDataMap.put(ThrownExperienceBottle.class, VanillaProjectileEntityInitializer.ExperienceBottle());
        vanillaDataMap.put(ThrownLingeringPotion.class, VanillaProjectileEntityInitializer.ThrowablePotion());
        vanillaDataMap.put(ThrownSplashPotion.class, VanillaProjectileEntityInitializer.ThrowablePotion());
        vanillaDataMap.put(FishingHook.class, VanillaProjectileEntityInitializer.FishingBobber());
        vanillaDataMap.put(FireworkRocketEntity.class, VanillaProjectileEntityInitializer.FireworkRocket());
        moddedDataMap = new HashMap<>(20);
    }

    /**
     * Returns projectile data for a given entity
     * @param projectileEntity entity to get data for
     * @return if data cannot be found, returns null
     */
    public ProjectileData getProjectileData(Entity projectileEntity) {
        if (!moddedDataMap.containsKey(projectileEntity.getClass())) {
            return vanillaDataMap.getOrDefault(projectileEntity.getClass(), VanillaProjectileEntityInitializer.Null()).init(projectileEntity);
        } else {
            return moddedDataMap.getOrDefault(projectileEntity.getClass(), ModdedProjectileEntityInitializer.Null()).init(projectileEntity);
        }
    }
}
