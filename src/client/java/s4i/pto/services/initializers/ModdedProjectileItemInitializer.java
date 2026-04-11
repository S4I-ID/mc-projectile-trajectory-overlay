package s4i.pto.services.initializers;

import net.minecraft.entity.player.PlayerEntity;
import s4i.pto.model.projectile.ItemData;
import s4i.pto.model.projectile.ProjectileData;

import java.util.List;

public interface ModdedProjectileItemInitializer {
    List<ProjectileData> init(float tickProgress, PlayerEntity firingEntity, ItemData heldItem);

    static ModdedProjectileItemInitializer Null() {
        return (tickProgress, firingEntity, heldItem) -> null;
    }
}
