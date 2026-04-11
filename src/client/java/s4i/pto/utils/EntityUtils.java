package s4i.pto.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import s4i.pto.mixins.EntityAccessor;

import java.util.List;

public class EntityUtils {
    /**
     * <p> Returns vehicle velocity vector  </p>
     *
     * @see Entity#getMovement()
     * @see ServerPlayerEntity#getMovement()
     * @see net.minecraft.entity.projectile.ProjectileEntity#spawnWithVelocity
     * @see ProjectileEntity#setVelocity(Entity, float, float, float, float, float)
     **/
    public static Vec3d getEntityMovement(LivingEntity firingEntity) {
        Entity vehicle = firingEntity.getVehicle();
        if (vehicle != null) {
            return vehicle.getControllingPassenger() != firingEntity || !ModUtils.isClassOrSuperClass(firingEntity, PlayerEntity.class) ?
                    vehicle.getMovement() : ((EntityAccessor) firingEntity).getRawMovement();
        } else {
            return ((EntityAccessor) firingEntity).getRawMovement();
        }
    }

    /**
     * @return true if given entity collides with a fluid block
     */
    public static boolean entityCollidesWithFluid(FluidState state, BlockPos fluidPos, Vec3d oldPos, Vec3d newPos, World world, Entity raycastEntity) {
        Box box = state.getCollisionBox(world, fluidPos);
        return box != null && entityCollidesWithBoxes(oldPos, newPos, raycastEntity, List.of(box));
    }

    /**
     * @return true if given entity collides with given boxes
     */
    public static boolean entityCollidesWithBoxes(Vec3d oldPos, Vec3d newPos, Entity raycastEntity, List<Box> boxes) {
        Box box = raycastEntity.getDimensions(raycastEntity.getPose()).getBoxAt(oldPos);
        Vec3d posDelta = newPos.subtract(oldPos);
        return box.collides(posDelta, boxes);
    }

    /**
     * Under certain conditions, some entities are immune to projectiles
     * @param entity entity to check
     * @return true if entity can be damaged by projectiles
     */
    public static boolean isEntityDamagedByProjectiles(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        LivingEntity livingEntity = entity.getEntity();
        if (livingEntity != null) {
            boolean isNotGuarding = !livingEntity.isBlocking();
            boolean isNotImmuneEntity = !ModUtils.isInClassesOrSuperClasses(entity, List.of(EndermanEntity.class, BreezeEntity.class));
            return isNotGuarding && isNotImmuneEntity;
        } else return ModUtils.isInClassesOrSuperClasses(entity, List.of(BlockAttachedEntity.class, VehicleEntity.class));
    }

    /**
     * Compares distances between a fixed point and two entity positions
     * @param e1 first entity
     * @param e2 second entity
     * @param startPos fixed point
     * @return -1 if distance from fixed point to e1 is smaller than the distance from the point to e2 <p>1 otherwise </p>
     */
    public static int comparatorEntityPosToPoint(Entity e1, Entity e2, Vec3d startPos) {
        if (startPos.distanceTo(e1.getEntityPos()) < startPos.distanceTo(e2.getEntityPos())) {
            return -1;
        } else {
            return 1;
        }
    }

    public static Box getLerpedBoundingBox(Entity entity, float tickProgress) {
        Vec3d boxOffset = entity.getLerpedPos(tickProgress).subtract(entity.getEntityPos());
        return entity.getBoundingBox().offset(boxOffset.x, boxOffset.y, boxOffset.z);
    }

    public static boolean isEntityHostile(Entity entity) {
        return ModUtils.isInClassesOrSuperClasses(entity, List.of(HostileEntity.class, PlayerEntity.class));
    }
}
