package s4i.pto.model.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import s4i.pto.model.OperationOrder;
import s4i.pto.utils.Utils;

import java.util.List;

public class ProjectileData {
    private int deviationId;
    private Vec3d velocity;
    private Vec3d velocityMin;
    private Vec3d velocityMax;
    private float yaw;
    private float pitch;
    private Vec3d position;
    private Vec3d lastPosition;
    private Vec3d gravityVelocity;
    private float airDrag;
    private float waterDrag;
    private float charge;
    private float inaccuracy;
    private List<OperationOrder> operationOrder;
    private Class<?> itemClassFiredFrom;
    private Entity entity;
    private boolean isInFluid;
    private int pierces;
    private int ticksLeft;
    private int tickDeviation;
    private int state;
    private boolean forceHighlightBlock;
    private boolean isExplosive;
    private int explosions;

    private ProjectileData() {}

    public ProjectileData(ProjectileData other, int deviationId, boolean isMinDeviation) {
        this.deviationId = deviationId;
        Vec3d velocity;
        int ticksLeft;
        if (deviationId > -1) {
            velocity = isMinDeviation ? other.velocityMin : other.velocityMax;
            ticksLeft = isMinDeviation ? other.ticksLeft - other.tickDeviation : other.ticksLeft + other.tickDeviation;
        } else {
            velocity = other.velocity;
            ticksLeft = other.ticksLeft;
            this.velocityMin = other.velocityMin;
            this.velocityMax = other.velocityMax;
        }
        this.velocity = Utils.copyVec3d(velocity);
        this.yaw = other.yaw;
        this.pitch = other.pitch;
        this.position = Utils.copyVec3d(other.position);
        this.lastPosition = other.lastPosition;
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

    public void addToPosition(Vec3d relativePos) {
        this.position = position.add(relativePos);
    }

    public void subtractTick() {
        this.ticksLeft = this.ticksLeft - 1;
    }


    public int getPierces() {
        return this.pierces;
    }

    public Vec3d getLastPosition() {
        return this.lastPosition;
    }

    public void setLastPosition(Vec3d lastPosition) {
        this.lastPosition = lastPosition;
    }

    public boolean isExplosive() {
        return this.isExplosive;
    }

    public int getExplosions() {
        return this.explosions;
    }

    public boolean isForceHighlightBlock() {
        return this.forceHighlightBlock;
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getTicksLeft() {
        return this.ticksLeft;
    }

    public int getDeviationId() {
        return this.deviationId;
    }

    public boolean isDeviation() {
        return this.deviationId > -1;
    }

    public boolean isInFluid() {
        return isInFluid;
    }

    public void setIsInFluid(boolean isInFluid) {
        this.isInFluid = isInFluid;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Vec3d getVelocity() {
        return velocity;
    }

    public void setVelocity(Vec3d velocity) {
        this.velocity = velocity;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Vec3d getPosition() {
        return position;
    }

    public Vec3d getGravityVelocity() {
        return gravityVelocity;
    }

    public float getAirDrag() {
        return airDrag;
    }

    public float getWaterDrag() {
        return waterDrag;
    }

    public float getCharge() {
        return charge;
    }

    public List<OperationOrder> getOperationOrder() {
        return operationOrder;
    }

    public Class<?> getItemClassFiredFrom() {
        return itemClassFiredFrom;
    }

    public void setItemClassFiredFrom(Class<?> itemClassFiredFrom) {
        this.itemClassFiredFrom = itemClassFiredFrom;
    }



    private ProjectileData(Builder builder) {
        this.deviationId = builder.deviationId;
        this.velocity = builder.projectileSpawnData.getVelocity();
        this.velocityMin = builder.projectileSpawnData.getVelocityWithMaxNegativeDeviation();
        this.velocityMax = builder.projectileSpawnData.getVelocityWithMaxPositiveDeviation();
        this.yaw = builder.projectileSpawnData.getYaw();
        this.pitch = builder.projectileSpawnData.getPitch();
        this.position = builder.position;
        this.lastPosition = builder.position;
        this.gravityVelocity = builder.gravityVelocity;
        this.airDrag = builder.airDrag;
        this.waterDrag = builder.waterDrag;
        this.charge = builder.charge;
        this.inaccuracy = builder.inaccuracy;
        this.operationOrder = builder.operationOrder;
        this.itemClassFiredFrom = builder.itemClassFrom;
        this.entity = builder.entity;
        this.isInFluid = builder.isInFluid;
        this.pierces = builder.pierces;
        this.ticksLeft = builder.ticksLeft;
        this.tickDeviation = builder.tickDeviation;
        this.state = builder.state;
        this.forceHighlightBlock = builder.forceHighlightBlock;
        this.isExplosive = builder.isExplosive;
        this.explosions = builder.explosions;
    }

    public static class Builder {
        private final int deviationId = -1;
        private ProjectileSpawnData projectileSpawnData;
        private Vec3d position;
        private Vec3d gravityVelocity;
        private float airDrag;
        private float waterDrag;
        private float charge = 1.0f;
        private float inaccuracy = 0f;
        private List<OperationOrder> operationOrder;
        private Class<?> itemClassFrom;
        private Entity entity;
        private final boolean isInFluid = false;
        private int pierces = 0;
        private int ticksLeft = 9999;
        private int tickDeviation = 0;
        private int state = 0;
        private boolean forceHighlightBlock = false;
        private boolean isExplosive = false;
        private int explosions = 0;

        public Builder projectileSpawnData(ProjectileSpawnData projectileSpawnData) {
            this.projectileSpawnData = projectileSpawnData;
            return this;
        }

        public Builder position(Vec3d position) {
            this.position = position;
            return this;
        }

        public Builder gravityVelocity(Vec3d gravityVelocity) {
            this.gravityVelocity = gravityVelocity;
            return this;
        }

        public Builder airDrag(float airDrag) {
            this.airDrag = airDrag;
            return this;
        }

        public Builder waterDrag(float waterDrag) {
            this.waterDrag = waterDrag;
            return this;
        }

        public Builder charge(float charge) {
            this.charge = charge;
            return this;
        }

        public Builder inaccuracy(float inaccuracy) {
            this.inaccuracy = inaccuracy;
            return this;
        }

        public Builder operationOrder(List<OperationOrder> operationOrder) {
            this.operationOrder = operationOrder;
            return this;
        }

        public Builder itemClassFrom(Class<?> itemClassFrom) {
            this.itemClassFrom = itemClassFrom;
            return this;
        }

        public Builder entity(Entity entity) {
            this.entity = entity;
            return this;
        }

        public Builder pierces(int pierces) {
            this.pierces = pierces;
            return this;
        }

        public Builder ticksLeft(int ticksLeft) {
            this.ticksLeft = ticksLeft;
            return this;
        }

        public Builder tickDeviation(int tickDeviation) {
            this.tickDeviation = tickDeviation;
            return this;
        }

        public Builder state(int state) {
            this.state = state;
            return this;
        }

        public Builder forceHighlightBlock(boolean forceHighlightBlock) {
            this.forceHighlightBlock = forceHighlightBlock;
            return this;
        }

        public Builder isExplosive(boolean explosive) {
            isExplosive = explosive;
            return this;
        }

        public Builder explosions(int explosions) {
            this.explosions = explosions;
            return this;
        }

        public ProjectileData build() {
            return new ProjectileData(this);
        }
    }
}
