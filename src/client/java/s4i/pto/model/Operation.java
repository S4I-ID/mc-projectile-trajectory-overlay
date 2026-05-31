package s4i.pto.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * <p> Abstracted projectile updates during a tick </p>
 * <p> Since 1.21, most items have a different order in which these are calculated per tick </p>
 * @see Entity#tick()
 * @see Projectile#tick()
 * @see AbstractArrow#tick()
 * @see ThrowableProjectile#tick()
 * @see Constants#RANGED_OPERATION_ORDER
 * @see Constants#THROWABLE_OPERATION_ORDER
 */
public enum Operation {
    VELOCITY,
    DRAG,
    POSITION,
    INITIAL_THROWN_BUBBLE_COLLISION,
    BLOCK_COLLISION,
    ROTATION,
    FISHING_BOBBER,
    FISHING_BOBBER_VELOCITY,
    UPDATE_WATER_STATE,
    ENTITY_COLLISION, BLOCK_RAYCAST, LIFETIME_UPDATE, MOVEMENT
}