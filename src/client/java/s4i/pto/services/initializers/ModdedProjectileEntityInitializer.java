package s4i.pto.services.initializers;

import net.minecraft.world.entity.Entity;
import s4i.pto.model.projectile.ProjectileData;

public interface ModdedProjectileEntityInitializer {
    ProjectileData init(Entity projectile);

    static ModdedProjectileEntityInitializer Null() {
        return (_) -> null;
    }
}
