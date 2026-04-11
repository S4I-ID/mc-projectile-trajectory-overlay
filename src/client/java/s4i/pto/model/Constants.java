package s4i.pto.model;

import java.util.List;

import static s4i.pto.model.OperationOrder.BLOCK_COLLISION;
import static s4i.pto.model.OperationOrder.BLOCK_RAYCAST;
import static s4i.pto.model.OperationOrder.DRAG;
import static s4i.pto.model.OperationOrder.ENTITY_COLLISION;
import static s4i.pto.model.OperationOrder.FISHING_BOBBER;
import static s4i.pto.model.OperationOrder.LIFETIME_UPDATE;
import static s4i.pto.model.OperationOrder.POSITION;
import static s4i.pto.model.OperationOrder.ROTATION;
import static s4i.pto.model.OperationOrder.UPDATE_WATER_STATE;
import static s4i.pto.model.OperationOrder.VELOCITY;

public class Constants {
    public static final String MOD_ID = "projectile-trajectory-overlay";

    // SPEED / POWER
    public static final float BOW_ARROW_MAX_SPEED = 3.0f;
    public static final float CROSSBOW_ARROW_MAX_SPEED = 3.15f;
    public static final float CROSSBOW_FIREWORK_MAX_SPEED = 1.6f;
    public static final float DEFAULT_THROWABLE_SPEED = 1.5f;
    public static final float EXP_BOTTLE_SPEED = 0.7f;
    public static final float POTION_BOTTLE_SPEED = 0.5f;

    public static final double WATER_SPEED = 0.014d;
    public static final double SLOW_LAVA_SPEED = 0.0023333333333333335d;
    public static final double FAST_LAVA_SPEED = 0.007d;

    // GRAVITY
    public static final float EXP_BOTTLE_GRAVITY = -0.07f;
    public static final float DROP_ITEM_GRAVITY = -0.04f;
    public static final float LLAMA_SPIT_GRAVITY = -0.06f;
    public static final float PERSISTENT_PROJECTILE_GRAVITY = -0.05f;
    public static final float POTION_GRAVITY = -0.05f;
    public static final float FIREWORK_GRAVITY = 0.04f;
    public static final float THROWABLE_GRAVITY = -0.03f;
    public static final float COLUMN_BUBBLE_SOUL_SAND = 0.06f;
    public static final float COLUMN_BUBBLE_SOUL_SAND_SURFACE = 0.1f;
    public static final float COLUMN_BUBBLE_MAGMA = -0.03f;

    // DRAG
    public static final float PERSISTENT_PROJECTILE_AIR_DRAG = 0.99f;
    public static final float PERSISTENT_PROJECTILE_WATER_DRAG = 0.6f;
    public static final float TRIDENT_WATER_DRAG = 0.99f;
    public static final float THROWABLE_AIR_DRAG = 0.99f;
    public static final float THROWABLE_WATER_DRAG = 0.8f;
    public static final float FIREBALL_DRAG = 0.95f;
    public static final float DWITHER_SKULLS_DRAG = 0.73f;
    public static final float FISHING_BOBBER_DRAG = 0.92f;
    public static final float DROP_ITEM_DRAG = 0.98f;

    // INACCURACY
    public static final float DEFAULT_PLAYER_DEVIATION = 1.0f;
    public static final float MAX_DEVIATION = 0.0172275f;
    public static final float FISHING_BOBBER_MAX_DEVIATION = 0.0103365f;
    public static final int FIREWORK_ROCKET_MAX_LIFETIME_DEVIATION = 13;

    // VELOCITY
    public static final float DROP_ITEM_TERMINAL_VELOCITY = 1.96f;
    public static final float FISHING_BOBBER_TERMINAL_VELOCITY = 0.345f;
    public static final float THROWN_SNOWBALL_TERMINAL_VELOCITY = 2.97f;
    public static final float THROWN_POTION_TERMINAL_VELOCITY = 4.95f;
    public static final float THROWN_EXP_BOTTLE_TERMINAL_VELOCITY = 6.93f;
    public static final float FIREBALL_TERMINAL_VELOCITY = 1.90f;
    public static final float DWITHER_SKULLS_TERMINAL_VELOCITY = 0.2703703f;
    public static final float WIND_CHARGE_TERMINAL_VELOCITY = Float.MAX_VALUE;
    public static final float LLAMA_SPIT_TERMINAL_VELOCITY = 6.0f;
    public static final float PERSISTENT_PROJECTILE_TERMINAL_VELOCITY = 5.0f;

    public static final double FIREWORK_EXPLOSION_RANGE = 5.0d;

    public static final List<OperationOrder> THROWABLE_OPERATION_ORDER =
            List.of(VELOCITY, DRAG, POSITION, ROTATION, BLOCK_COLLISION, UPDATE_WATER_STATE, ENTITY_COLLISION, BLOCK_RAYCAST);
    public static final List<OperationOrder> RANGED_OPERATION_ORDER =
            List.of(POSITION, DRAG, ROTATION, VELOCITY, BLOCK_COLLISION, UPDATE_WATER_STATE, ENTITY_COLLISION, BLOCK_RAYCAST);
    public static final List<OperationOrder> FIREWORK_OPERATION_ORDER =
            List.of(POSITION, ENTITY_COLLISION, BLOCK_RAYCAST, LIFETIME_UPDATE);
    public static final List<OperationOrder> FISHING_BOBBER_OPERATION_ORDER =
            List.of(FISHING_BOBBER, VELOCITY, BLOCK_COLLISION, ROTATION, DRAG, POSITION, UPDATE_WATER_STATE, ENTITY_COLLISION, BLOCK_RAYCAST);
}
