package s4i.pto.services.initializers;

import net.minecraft.entity.Entity;
import s4i.pto.model.projectile.ProjectileData;

public interface ModdedProjectileEntityInitializer {
    ProjectileData init(Entity projectile);

    static ModdedProjectileEntityInitializer Null() {
        return (projectile -> null);
    }
}
