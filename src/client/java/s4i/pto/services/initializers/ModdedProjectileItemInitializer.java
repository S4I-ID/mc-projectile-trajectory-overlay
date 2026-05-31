package s4i.pto.services.initializers;

import net.minecraft.world.entity.player.Player;
import s4i.pto.model.projectile.ItemData;
import s4i.pto.model.projectile.ProjectileData;

import java.util.List;

public interface ModdedProjectileItemInitializer {
    List<ProjectileData> init(float tickProgress, Player firingEntity, ItemData heldItem);

    static ModdedProjectileItemInitializer Null() {
        return (_, _, _) -> null;
    }
}
