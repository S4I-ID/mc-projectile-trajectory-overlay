package s4i.pto.model;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;

/**
 * <p> Abstracted projectile updates during a tick </p>
 * <p> Since 1.21, most items have a different order in which these are calculated per tick </p>
 * @see Entity#tick()
 * @see PersistentProjectileEntity#tick()
 * @see ThrownEntity#tick()
 * @see Constants#RANGED_OPERATION_ORDER
 * @see Constants#THROWABLE_OPERATION_ORDER
 */
public enum OperationOrder {
    VELOCITY,
    DRAG,
    WATER_DRAG,
    AIR_DRAG,
    POSITION,
    INITIAL_THROWN_BUBBLE_COLLISION,
    BLOCK_COLLISION,
    ROTATION,
    FISHING_BOBBER,
    FISHING_BOBBER_VELOCITY,
    UPDATE_WATER_STATE,
    ENTITY_COLLISION, BLOCK_RAYCAST, LIFETIME_UPDATE, MOVEMENT
}