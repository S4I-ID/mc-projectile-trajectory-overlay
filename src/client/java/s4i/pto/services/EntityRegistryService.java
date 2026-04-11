package s4i.pto.services;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.LingeringPotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
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
        vanillaDataMap.put(ArrowEntity.class, VanillaProjectileEntityInitializer.Arrow());
        vanillaDataMap.put(TridentEntity.class, VanillaProjectileEntityInitializer.Trident());
        vanillaDataMap.put(EggEntity.class, VanillaProjectileEntityInitializer.DefaultThrowable());
        vanillaDataMap.put(SnowballEntity.class, VanillaProjectileEntityInitializer.DefaultThrowable());
        vanillaDataMap.put(EnderPearlEntity.class, VanillaProjectileEntityInitializer.EnderPearl());
        vanillaDataMap.put(ExperienceBottleEntity.class, VanillaProjectileEntityInitializer.ExperienceBottle());
        vanillaDataMap.put(LingeringPotionEntity.class, VanillaProjectileEntityInitializer.ThrowablePotion());
        vanillaDataMap.put(SplashPotionEntity.class, VanillaProjectileEntityInitializer.ThrowablePotion());
        vanillaDataMap.put(FishingBobberEntity.class, VanillaProjectileEntityInitializer.FishingBobber());
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
