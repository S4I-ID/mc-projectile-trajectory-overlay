package s4i.pto.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EntityUtils {
    /**
     * Simulates server-side velocity calculation
     * @param player player to get server movement for
     * @return Vec3 of player's server-side movement speed
     * @see ServerPlayer#getKnownMovement()
     */
    public static Vec3 getEntityServerMovement(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null && vehicle.getControllingPassenger() != entity) {
            return vehicle.getKnownMovement();
        }
        return entity.getKnownMovement();
    }

    /**
     * @return true if given entity collides with a fluid block
     */
    public static boolean entityCollidesWithFluid(FluidState state, BlockPos fluidPos, Vec3 oldPos, Vec3 newPos, Level world, Entity raycastEntity) {
        AABB box = state.getAABB(world, fluidPos);
        return box != null && entityCollidesWithBoxes(oldPos, newPos, raycastEntity, List.of(box));
    }

    /**
     * @return true if given entity collides with given boxes
     */
    public static boolean entityCollidesWithBoxes(Vec3 oldPos, Vec3 newPos, Entity raycastEntity, List<AABB> boxes) {
        AABB box = raycastEntity.getDimensions(raycastEntity.getPose()).makeBoundingBox(oldPos);
        Vec3 posDelta = newPos.subtract(oldPos);
        return box.collidedAlongVector(posDelta, boxes);
    }

    /**
     * Under certain conditions, some entities are immune to projectiles
     *
     * @param entity entity to check
     *
     * @return true if entity can be damaged by projectiles
     */
    public static boolean isEntityDamagedByProjectiles(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        LivingEntity livingEntity = entity.asLivingEntity();
        if (livingEntity != null) {
            boolean isNotGuarding = !livingEntity.isBlocking();
            boolean isNotImmuneEntity = !ModUtils.isInClassesOrSuperClasses(entity, List.of(EnderMan.class, Breeze.class));
            return isNotGuarding && isNotImmuneEntity;
        } else return ModUtils.isInClassesOrSuperClasses(entity, List.of(BlockAttachedEntity.class, VehicleEntity.class));
    }

    /**
     * Compares distances between a fixed point and two entity positions
     *
     * @param e1       first entity
     * @param e2       second entity
     * @param startPos fixed point
     *
     * @return -1 if distance from fixed point to e1 is smaller than the distance from the point to e2 <p> 1 otherwise </p>
     */
    public static int comparatorEntityPosToPoint(Entity e1, Entity e2, Vec3 startPos) {
        if (startPos.distanceTo(e1.position()) < startPos.distanceTo(e2.position())) {
            return -1;
        } else {
            return 1;
        }
    }

    public static AABB getLerpedBoundingBox(Entity entity, float tickProgress) {
        Vec3 boxOffset = entity.getPosition(tickProgress).subtract(entity.position());
        return entity.getBoundingBox().move(boxOffset.x, boxOffset.y, boxOffset.z);
    }

    public static boolean isEntityHostile(Entity entity) {
        return ModUtils.isInClassesOrSuperClasses(entity, List.of(Monster.class, Player.class));
    }
}
