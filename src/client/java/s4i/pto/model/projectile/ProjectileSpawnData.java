package s4i.pto.model.projectile;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Accessors(fluent = false, chain = true)
@Data
@NoArgsConstructor
public class ProjectileSpawnData {
    private Vec3 velocity;
    private Vec3 velocityWithMaxNegativeDeviation;
    private Vec3 velocityWithMaxPositiveDeviation;
    private float yaw;
    private float pitch;

    public ProjectileSpawnData(Entity entity) {
        this.velocity = entity.getDeltaMovement();
        this.yaw = entity.getXRot();
        this.pitch = entity.getYRot();
    }
}
