package s4i.pto.model.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class ProjectileSpawnData {
    private Vec3d velocity;
    private Vec3d velocityWithMaxNegativeDeviation;
    private Vec3d velocityWithMaxPositiveDeviation;
    private float yaw;
    private float pitch;

    public ProjectileSpawnData() {}

    public ProjectileSpawnData(Entity entity) {
        this.velocity = entity.getVelocity();
        this.yaw = entity.getYaw();
        this.pitch = entity.getPitch();
    }

    public Vec3d getVelocity() {
        return velocity;
    }

    public ProjectileSpawnData setVelocity(Vec3d velocity) {
        this.velocity = velocity;
        return this;
    }

    public Vec3d getVelocityWithMaxNegativeDeviation() {
        return velocityWithMaxNegativeDeviation;
    }

    public ProjectileSpawnData setVelocityWithMaxNegativeDeviation(Vec3d velocityWithMaxNegativeDeviation) {
        this.velocityWithMaxNegativeDeviation = velocityWithMaxNegativeDeviation;
        return this;
    }

    public Vec3d getVelocityWithMaxPositiveDeviation() {
        return velocityWithMaxPositiveDeviation;
    }

    public ProjectileSpawnData setVelocityWithMaxPositiveDeviation(Vec3d velocityWithMaxPositiveDeviation) {
        this.velocityWithMaxPositiveDeviation = velocityWithMaxPositiveDeviation;
        return this;
    }

    public float getYaw() {
        return yaw;
    }

    public ProjectileSpawnData setYaw(float yaw) {
        this.yaw = yaw;
        return this;
    }

    public float getPitch() {
        return pitch;
    }

    public ProjectileSpawnData setPitch(float pitch) {
        this.pitch = pitch;
        return this;
    }
}
