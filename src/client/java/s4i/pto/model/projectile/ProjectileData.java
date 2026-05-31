package s4i.pto.model.projectile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import s4i.pto.model.Operation;
import s4i.pto.utils.Utils;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class ProjectileData {
    @Default
    private int deviationId = -1;
    private Vec3 velocity;
    private Vec3 velocityMin;
    private Vec3 velocityMax;
    private float yaw;
    private float pitch;
    private Vec3 position;
    private Vec3 lastPosition;
    private Vec3 gravityVelocity;
    private float airDrag;
    private float waterDrag;
    @Default
    private float charge = 1.0f;
    @Default
    private float inaccuracy = 0f;
    private List<Operation> operationOrder;
    private Class<?> itemClassFiredFrom;
    private Entity entity;
    @Default
    private boolean isInFluid = false;
    @Default
    private int pierces = 0;
    @Default
    private int ticksLeft = 9999;
    @Default
    private int tickDeviation = 0;
    @Default
    private int state = 0;
    @Default
    private boolean forceHighlightBlock = false;
    @Default
    private boolean isExplosive = false;
    @Default
    private int explosions = 0;

    public ProjectileData(ProjectileData other, int deviationId, boolean isMinDeviation) {
        this.deviationId = deviationId;
        Vec3 velocity;
        int ticksLeft;
        if (deviationId > -1) {
            velocity = isMinDeviation ? other.velocityMin : other.velocityMax;
            ticksLeft = isMinDeviation ? other.ticksLeft - other.tickDeviation : other.ticksLeft + other.tickDeviation;
        } else {
            velocity = other.velocity;
            ticksLeft = other.ticksLeft;
            this.velocityMin = Utils.copyVec3d(other.velocityMin);
            this.velocityMax = Utils.copyVec3d(other.velocityMax);
        }
        this.velocity = Utils.copyVec3d(velocity);
        this.yaw = other.yaw;
        this.pitch = other.pitch;
        this.position = Utils.copyVec3d(other.position);
        this.lastPosition = Utils.copyVec3d(other.lastPosition);
        this.gravityVelocity = other.gravityVelocity;
        this.airDrag = other.airDrag;
        this.waterDrag = other.waterDrag;
        this.charge = other.charge;
        this.inaccuracy = other.inaccuracy;
        this.operationOrder = other.operationOrder;
        this.itemClassFiredFrom = other.itemClassFiredFrom;
        this.entity = other.entity;
        this.isInFluid = other.isInFluid;
        this.pierces = other.pierces;
        this.ticksLeft = ticksLeft;
        this.tickDeviation = other.tickDeviation;
        this.state = other.state;
        this.forceHighlightBlock = other.forceHighlightBlock;
        this.isExplosive = other.isExplosive;
        this.explosions = other.explosions;
    }

    public void subtractPierce() {
        this.pierces = this.pierces - 1;
    }

    public void addToPosition(Vec3 relativePos) {
        this.position = position.add(relativePos);
    }

    public void subtractTick() {
        this.ticksLeft = this.ticksLeft - 1;
    }

    public boolean isDeviation() {
        return deviationId > -1;
    }

    public static class ProjectileDataBuilder {
        public ProjectileDataBuilder projectileSpawnData(ProjectileSpawnData projectileSpawnData) {
            this.velocity = projectileSpawnData.getVelocity();
            this.velocityMin = projectileSpawnData.getVelocityWithMaxNegativeDeviation();
            this.velocityMax = projectileSpawnData.getVelocityWithMaxPositiveDeviation();
            this.yaw = projectileSpawnData.getYaw();
            this.pitch = projectileSpawnData.getPitch();
            return this;
        }

        public ProjectileDataBuilder position(Vec3 position) {
            this.position = position;
            this.lastPosition = position;
            return this;
        }
    }
}
