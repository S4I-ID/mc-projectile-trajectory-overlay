package s4i.pto.services.tasks;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import s4i.pto.config.ModConfig;
import s4i.pto.mixins.EntityAccessor;
import s4i.pto.model.Constants;
import s4i.pto.model.LineSource;
import s4i.pto.model.Operation;
import s4i.pto.model.OperationResult;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.model.simulation.Line;
import s4i.pto.model.simulation.LineSegment;
import s4i.pto.model.simulation.PredictionResult;
import s4i.pto.model.simulation.RenderBox;
import s4i.pto.utils.ColorUtils;
import s4i.pto.utils.EntityUtils;
import s4i.pto.utils.ModUtils;
import s4i.pto.utils.ProjectileUtils;
import s4i.pto.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static s4i.pto.model.Constants.RAD_TO_DEG;

public class PredictionTask implements Callable<PredictionResult> {
    /**
     * Some projectiles do not use their overriden on bubble column collision function and use the {@link Entity#onInsideBubbleColumn(boolean) base class} one instead
     */
    private static final Set<Class<?>> cappedBubbleDragItems = Set.of(EnderpearlItem.class);
    private static final Set<Class<?>> explosiveProjectileEntities = Set.of(FireworkRocketEntity.class);
    private static final List<Class<?>> entitiesWithCollision = List.of(LivingEntity.class, BlockAttachedEntity.class, VehicleEntity.class);
    private static final ModConfig config = ModConfig.getInstance();

    private final ProjectileData projectileData;
    private final Player firingEntity;
    private final LineSource lineSource;
    private final Level level;
    private final float tickProgress;

    public PredictionTask(ProjectileData projectile, Player firingEntity, LineSource lineSource, Level level, float tickProgress) {
        this.projectileData = projectile;
        this.firingEntity = firingEntity;
        this.lineSource = lineSource;
        this.level = level;
        this.tickProgress = tickProgress;
    }

    @Override
    public PredictionResult call() throws Exception {
        PredictionResult predictionResult = calculateTrajectoryLine();
        if (projectileData.isExplosive() && config.showExplosionRadius) {
            simulateExplosion(predictionResult.getEntityMap());
        }
        return predictionResult;
    }

    /**
     * Simulates explosion at the end of the projectile. Used for fireworks.
     *
     * @see FireworkRocketEntity#explode(ServerLevel)
     */
    private void simulateExplosion(Map<Entity, RenderBox> entityMap) {
        if (explosiveProjectileEntities.contains(projectileData.getEntity().getClass())) {
            Vec3 lastPosition = projectileData.getLastPosition();
            if (projectileData.getExplosions() <= 0) {
                return;
            }
            AABB explosionBox = projectileData.getEntity().getBoundingBox().inflate(Constants.FIREWORK_EXPLOSION_RANGE);
            entityMap.computeIfAbsent(new Arrow(EntityType.ARROW, level), k -> new RenderBox(explosionBox,
                    ColorUtils.getDeviationColor(config, lineSource), ColorUtils.getDeviationColor(config, lineSource)));

            for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, explosionBox)) {
                if (lastPosition.distanceToSqr(livingEntity.position()) <= Constants.FIREWORK_EXPLOSION_RANGE * Constants.FIREWORK_EXPLOSION_RANGE) {
                    boolean hitsEntity = false;
                    for (int i = 0; i < 2; ++i) {
                        Vec3 entityCheckPos = new Vec3(livingEntity.getX(), livingEntity.getY(0.5d * i), livingEntity.getZ());
                        HitResult hitResult = level.clip(
                                new ClipContext(lastPosition, entityCheckPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectileData.getEntity()));
                        if (hitResult.getType() == HitResult.Type.MISS) {
                            hitsEntity = true;
                            break;
                        }
                    }
                    if (hitsEntity) {
                        boolean isHostile = EntityUtils.isEntityHostile(livingEntity);
                        entityMap.computeIfAbsent(livingEntity, k -> new RenderBox(EntityUtils.getLerpedBoundingBox(k, tickProgress),
                                config.highlightTargetedEntity ? ColorUtils.getHighlightEntityColor(config, lineSource, isHostile) : null,
                                config.highlightTargetedEntityEdges ? ColorUtils.getHighlightEntityOutlineColor(config, lineSource, isHostile) : null));
                    }
                }
            }
        }
    }

    private PredictionResult calculateTrajectoryLine() {
        PredictionResult predictionResult = new PredictionResult();
        Line line = new Line(projectileData.getDeviationId(), lineSource);
        List<LineSegment> vertices = line.getSegments();
        predictionResult.getLines().add(line);

        boolean isDeviation = projectileData.isDeviation();     // if is deviation, discard boxes
        Map<Entity, RenderBox> entityMap = !isDeviation ? predictionResult.getEntityMap() : new HashMap<>();
        Map<BlockPos, RenderBox> blockMap = !isDeviation ? predictionResult.getBlockMap() : new HashMap<>();

        Entity raycastEntity = projectileData.getEntity();
        raycastEntity.setPos(projectileData.getPosition());
        raycastEntity.setXRot(projectileData.getYaw());
        raycastEntity.setYRot(projectileData.getPitch());

        for (int tick = 0; tick < config.maxNumberOfTicksToSimulate; tick++) {
            for (Operation operation : projectileData.getOperationOrder()) {
                OperationResult result = switch (operation) {
                    case DRAG -> applyDrag();
                    case VELOCITY -> applyVelocity();
                    case POSITION -> applyPosition();
                    case INITIAL_THROWN_BUBBLE_COLLISION -> applyInitialThrownEntityBubbleColumnCollision(tick);
                    case BLOCK_COLLISION -> applyBlockCollisions();
                    case ROTATION -> applyRotation();
                    case FISHING_BOBBER -> applyFishingBobberBuoyancy();
                    case UPDATE_WATER_STATE -> applyFluidVelocity();
                    case ENTITY_COLLISION -> applyEntityCollision(entityMap, line, tick);
                    case BLOCK_RAYCAST -> applyBlockRaycast(blockMap, line);
                    case LIFETIME_UPDATE -> applyLifetimeUpdate();
                    case null, default -> throw new UnsupportedOperationException(String.format("Unknown estimation case: %s", operation));
                };
                if (result == OperationResult.RETURN) {
                    return predictionResult;
                }
                if (result == OperationResult.BREAK) {
                    tick = config.maxNumberOfTicksToSimulate;
                }
            }
            line.addVertex(projectileData.getLastPosition(), projectileData.getPosition(),
                    ColorUtils.getLineColor(config, projectileData.getCharge(), lineSource));
        }
        if (config.highlightLandingBlock && projectileData.isForceHighlightBlock() && !vertices.isEmpty()) {
            Vec3 lastPos = vertices.getLast().end;
            BlockPos blockPos = new BlockPos(Mth.floor(lastPos.x), Mth.floor(lastPos.y), Mth.floor(lastPos.z));
            blockMap.computeIfAbsent(blockPos, _ ->
                    new RenderBox(new AABB(blockPos)).addOutlineColor(ColorUtils.getHighlightBlockColor(config, lineSource)));
        }
        return predictionResult;
    }


    /**
     * Applies the drag operation depending on the liquid submersion state of the entity
     */
    private OperationResult applyDrag() {
        float drag = projectileData.isInFluid() ? projectileData.getWaterDrag() : projectileData.getAirDrag();
        projectileData.setVelocity(projectileData.getVelocity().scale(drag));
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Adds updates to projectile's velocity, usually gravitational force  </p>
     * <p> Fishing bobbers use a different movement function </p>
     *
     * @see FishingHook#tick()
     */
    private OperationResult applyVelocity() {
        if (projectileData.getItemClassFiredFrom().equals(FishingRodItem.class)) {
            applyFishingBobberMovement();
        } else {
            projectileData.setVelocity(projectileData.getVelocity().add(projectileData.getGravityVelocity()));
        }
        return OperationResult.CONTINUE;
    }

    /**
     * Fishing bobber uses a certain part of the underlying Entity class for velocity calculation
     *
     * @see TridentItem#releaseUsing(ItemStack, Level, LivingEntity, int)
     * @see FishingHook#tick()
     * @see Entity#move
     */
    private void applyFishingBobberMovement() {
        Vec3 pos = projectileData.getPosition();
        BlockPos blockPos = new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
        BlockState blockState = level.getBlockState(blockPos);
        float f = blockState.getBlock().getSpeedFactor();
        float x;
        if (!blockState.is(Blocks.WATER) && !blockState.is(Blocks.BUBBLE_COLUMN)) {
            x = f == 1.0f ?
                    level.getBlockState(new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y - 0.500001f), Mth.floor(pos.z))).getBlock().getSpeedFactor() : f;
        } else {
            x = f;
        }
        projectileData.setVelocity(projectileData.getVelocity().multiply(x, 1.0, x));
    }

    /**
     * Updates entity position according to current velocity
     */
    private OperationResult applyPosition() {
        projectileData.setLastPosition(projectileData.getPosition());
        projectileData.addToPosition(projectileData.getVelocity());
        projectileData.getEntity().setPos(projectileData.getPosition());
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Thrown items get this check at the beginning of their tick  </p>
     *
     * @see ThrowableProjectile#tick()
     * @see ThrowableProjectile#handleFirstTickBubbleColumn()
     */
    private OperationResult applyInitialThrownEntityBubbleColumnCollision(int tick) {
        if (tick == 0 && config.useComplexPhysics) {
            Entity raycastEntity = projectileData.getEntity();
            for (BlockPos blockPos : BlockPos.betweenClosed(raycastEntity.getBoundingBox())) {
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.is(Blocks.BUBBLE_COLUMN)) {
                    BlockState airBlock = level.getBlockState(blockPos.above());
                    boolean isSurfaceBubbleColumn = airBlock.getCollisionShape(level, blockPos).isEmpty() && airBlock.getFluidState().isEmpty();
                    boolean isDownward = blockState.getValue(BubbleColumnBlock.DRAG_DOWN);
                    double bubbleDrag = ProjectileUtils.getBubbleDragForProjectiles(isSurfaceBubbleColumn, isDownward);
                    projectileData.setVelocity(projectileData.getVelocity().add(0, bubbleDrag, 0));
                }
            }
        }
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Each block the entity intersects (even a little) will
     * {@link BlockBehaviour#entityInside(BlockState, Level, BlockPos, Entity, InsideBlockEffectApplier, boolean) apply its effects} on the passing entity </p>
     * <p> Collision checker used for entities, rewritten to use our data model </p>
     * <p> Checks up to 16 blocks plus the final block between our entity's starting and ending position </p>
     * <p> Player entities use this method differently, see attached function </p>
     *
     * @see Entity#checkInsideBlocks(List, InsideBlockEffectApplier.StepBasedCollector)
     * @see Entity#tick()
     */
    private OperationResult applyBlockCollisions() {
        if (config.useComplexPhysics) {
            Set<Long> visited = new HashSet<>(32);

            Vec3 from = Utils.copyVec3d(projectileData.getLastPosition());
            Vec3 to = projectileData.getPosition();
            int stepsLeft = 16;

            stepsLeft -= checkBlockCollision(from, to, visited, 16);
            if (stepsLeft <= 0) {       // if entity is moving very fast, collision check won't reach final block; do final block check
                checkBlockCollision(to, to, visited, 1);
            }
        }
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Modified block collision checker </p>
     * <p> We are only interested in blocks that affect our projectile's flight path: underwater bubble columns </p>
     *
     * @see Entity#checkInsideBlocks(Vec3, Vec3, InsideBlockEffectApplier.StepBasedCollector, LongSet, int)
     */
    private int checkBlockCollision(Vec3 from, Vec3 to, Set<Long> visited, int stepsLeft) {
        Entity raycastEntity = projectileData.getEntity();
        AABB box = ((EntityAccessor) raycastEntity).getDimensions().makeBoundingBox(to).deflate(0.00001f);
        AtomicInteger steps = new AtomicInteger();
        BlockGetter.forEachBlockIntersectedBetween(from, to, box, (BlockPos blockPos, int j) -> {
            if (j >= stepsLeft)
                return false;
            else {
                steps.set(j);
                BlockState blockState = level.getBlockState(blockPos);
                if (!blockState.isAir()) {
                    boolean bl4 = EntityUtils.entityCollidesWithFluid(blockState.getFluidState(), blockPos, from, to, level, raycastEntity);
                    if (bl4 && visited.add(blockPos.asLong())) {
                        if (blockState.is(Blocks.BUBBLE_COLUMN)) {
                            BlockPos airBlockPos = blockPos.above();
                            BlockState airBlock = level.getBlockState(airBlockPos);
                            boolean isSurfaceBubbleColumn = airBlock.getCollisionShape(level, airBlockPos).isEmpty() && airBlock.getFluidState().isEmpty();
                            applyBubbleColumn(isSurfaceBubbleColumn, blockState.getValue(BubbleColumnBlock.DRAG_DOWN));
                        }
                    }
                }
                return true;
            }
        });
        return steps.get() + 1;
    }

    /**
     * <p> Applies bubble column effect to our projectile </p>
     * <p> Some projectiles use the base Entity class effects </p>
     *
     * @see Entity#onInsideBubbleColumn(boolean)
     * @see Projectile#onInsideBubbleColumn(boolean)
     */
    private void applyBubbleColumn(boolean isSurfaceColumn, boolean isDownward) {
        if (cappedBubbleDragItems.contains(projectileData.getItemClassFiredFrom())) {
            double newY = ProjectileUtils.getBubbleVelocityUpdateForEntities(projectileData.getVelocity().y, isSurfaceColumn, isDownward);
            projectileData.setVelocity(projectileData.getVelocity().multiply(1, 0, 1).add(0, newY, 0));
        } else {
            double deltaY = ProjectileUtils.getBubbleDragForProjectiles(isSurfaceColumn, isDownward);
            projectileData.setVelocity(projectileData.getVelocity().add(0, deltaY, 0));
        }
    }

    /**
     * Updates raycastEntity rotation
     *
     * @see Projectile#updateRotation()
     */
    private OperationResult applyRotation() {
        Vec3 currentVelocity = projectileData.getVelocity();
        float newYaw = (float) (Mth.atan2(currentVelocity.x, currentVelocity.z) * RAD_TO_DEG);
        float newPitch = (float) (Mth.atan2(currentVelocity.y, currentVelocity.horizontalDistance()) * RAD_TO_DEG);
        float updatedYaw = ProjectileUtils.getNewEntityRotation(projectileData.getYaw(), newYaw);
        float updatedPitch = ProjectileUtils.getNewEntityRotation(projectileData.getPitch(), newPitch);
        projectileData.setYaw(updatedYaw);
        projectileData.setPitch(updatedPitch);
        Entity raycastEntity = projectileData.getEntity();
        raycastEntity.setXRot(updatedYaw);
        raycastEntity.setYRot(updatedPitch);
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Unique buoyancy used by fishing bobber </p>
     *
     * @see FishingHook#tick()
     * @see FishingHook.FishHookState
     */
    private OperationResult applyFishingBobberBuoyancy() {
        Level level = projectileData.getEntity().level();
        Vec3 pos = projectileData.getPosition();
        BlockPos blockPos = new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));

        if (projectileData.getState() == 0 && (projectileData.isInFluid() || !level.getFluidState(blockPos).isEmpty())) {
            projectileData.setState(-1);
            projectileData.setVelocity(projectileData.getVelocity().multiply(0.3f, 0.2f, 0.3f));
        } else if (projectileData.getState() != 0) {
            if (!projectileData.isInFluid()) {
                projectileData.setState(0);
            }
            Vec3 velocity = projectileData.getVelocity();

            FluidState fluidState = level.getFluidState(blockPos);
            double d = pos.y + velocity.y - blockPos.getY() - fluidState.getHeight(level, blockPos);
            if (Math.abs(d) < 0.01) {
                d += Math.signum(d) * 0.1;
            }
            projectileData.setVelocity(new Vec3(velocity.x * 0.9f, velocity.y - d * 0.1f, velocity.z * 0.9f));
        }
        if (!projectileData.isInFluid()) {
            projectileData.setVelocity(projectileData.getVelocity().add(projectileData.getGravityVelocity()));
        }
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Updates water state for our projectile. Adds weighted velocity of fluids to projectile. </p>
     * <p> Also sets the "isInFluid" flag. </p>
     *
     * @see Entity#baseTick()
     * @see Entity#updateFluidInteraction()
     * @see EntityFluidInteraction#update(Entity, boolean)
     */
    private OperationResult applyFluidVelocity() {
        boolean isInWater = updateMovementInFluid(FluidTags.WATER, Constants.WATER_SPEED);
        double lavaSpeed = level.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ?
                Constants.FAST_LAVA_SPEED : Constants.SLOW_LAVA_SPEED;
        boolean isInLava = updateMovementInFluid(FluidTags.LAVA, lavaSpeed);
        projectileData.setInFluid(isInWater || isInLava);
        return OperationResult.CONTINUE;
    }

    /**
     *
     * @param fluidTag   fluid tag to check
     * @param fluidSpeed fluid speed for velocity calculation
     *
     * @return true if entity's bounding box collides with the given fluid
     *
     * @see EntityFluidInteraction
     */
    private boolean updateMovementInFluid(TagKey<Fluid> fluidTag, double fluidSpeed) {
        Entity entity = projectileData.getEntity();
        if (entity.touchingUnloadedChunk()) {
            return false;
        } else {
            AABB box = entity.getBoundingBox().deflate(0.001);
            int i = Mth.floor(box.minX);
            int j = Mth.ceil(box.maxX);
            int k = Mth.floor(box.minY);
            int l = Mth.ceil(box.maxY);
            int m = Mth.floor(box.minZ);
            int n = Mth.ceil(box.maxZ);
            double waterHeightMultiplier = 0.0;
            boolean isPushedByFluids = entity.isPushedByFluid();
            boolean isInFluid = false;
            Vec3 totalFluidVelocity = Vec3.ZERO;
            int fluidBlocks = 0;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int p = i; p < j; p++) {
                for (int q = k; q < l; q++) {
                    for (int r = m; r < n; r++) {
                        mutable.set(p, q, r);
                        FluidState fluidState = level.getFluidState(mutable);
                        if (fluidState.is(fluidTag)) {
                            double waterBlockHeight = q + fluidState.getHeight(level, mutable);
                            if (waterBlockHeight >= box.minY) {
                                isInFluid = true;
                                waterHeightMultiplier = Math.max(waterBlockHeight - box.minY, waterHeightMultiplier);
                                if (isPushedByFluids) {
                                    Vec3 fluidVelocity = fluidState.getFlow(level, mutable);
                                    if (waterHeightMultiplier < 0.4) {
                                        fluidVelocity = fluidVelocity.scale(waterHeightMultiplier);
                                    }
                                    totalFluidVelocity = totalFluidVelocity.add(fluidVelocity);
                                    fluidBlocks++;
                                }
                            }
                        }
                    }
                }
            }
            if (totalFluidVelocity.length() > 0.0) {
                if (fluidBlocks > 0) {
                    totalFluidVelocity = totalFluidVelocity.scale(1.0 / fluidBlocks);
                }
                if (!ModUtils.isClassOrSuperClass(projectileData.getEntity(), Player.class)) {
                    totalFluidVelocity = totalFluidVelocity.normalize();
                }
                Vec3 initialVelocity = projectileData.getVelocity();
                totalFluidVelocity = totalFluidVelocity.scale(fluidSpeed);
                double f = 0.003;
                if (Math.abs(initialVelocity.x) < f && Math.abs(initialVelocity.z) < f && totalFluidVelocity.length() < 0.0045000000000000005) {
                    totalFluidVelocity = totalFluidVelocity.normalize().scale(0.0045000000000000005);
                }
                projectileData.setVelocity(initialVelocity.add(totalFluidVelocity));
            }
            return isInFluid;
        }
    }

    /**
     * <p> Handles entity collisions. Certain projectiles pierce through multiple entities (crossbow bolts) </p>
     *
     * @return stops the trajectory simulation if it cannot pierce any more entities
     */
    private OperationResult applyEntityCollision(Map<Entity, RenderBox> entityMap, Line line, int tick) {
        Vec3 startPos = projectileData.getLastPosition();
        Vec3 endPos = projectileData.getPosition();

        AABB entityCheckBox = new AABB(startPos, endPos).inflate(1.0d);    // get entities near projectile position
        List<Entity> entities = level.getEntities(firingEntity, entityCheckBox).stream()
                .filter(entity -> entity.isAlive() && ModUtils.isInClassesOrSuperClasses(entity, entitiesWithCollision))
                .sorted((e1, e2) -> EntityUtils.comparatorEntityPosToPoint(e1, e2, startPos))
                .collect(Collectors.toCollection(ArrayList::new));
        if (tick == 0) {
            entities.remove(firingEntity);
        }
        for (Entity entity : entities) {
            AABB boundingBox = EntityUtils.getLerpedBoundingBox(entity, tickProgress);
            if (boundingBox.inflate(entity.getPickRadius()).intersects(startPos, endPos)) {
                if (EntityUtils.isEntityDamagedByProjectiles(entity)) {
                    boolean isHostile = EntityUtils.isEntityHostile(entity);
                    entityMap.computeIfAbsent(entity, k -> new RenderBox(boundingBox,
                            config.highlightTargetedEntity ? ColorUtils.getHighlightEntityColor(config, lineSource, isHostile) : null,
                            config.highlightTargetedEntityEdges ? ColorUtils.getHighlightEntityOutlineColor(config, lineSource, isHostile) : null));
                } else {
                    entityMap.put(entity, null);
                }
                projectileData.subtractPierce();
                if (projectileData.getPierces() < 0) {          // handle piercing through entities (e.g. from crossbows)
                    line.addVertex(startPos, endPos, ColorUtils.getLineColor(config, projectileData.getCharge(), lineSource));
                    return OperationResult.RETURN;
                }
            }
        }
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Handles block raycasting for block hit collision detection. </p>
     * <p> If complex simulation is turned off, also handle water block detection </p>
     *
     * @return stops the trajectory simulation if entity hitsEntity a solid block
     */
    private OperationResult applyBlockRaycast(Map<BlockPos, RenderBox> blockMap, Line line) {
        Vec3 startPos = projectileData.getLastPosition();
        Vec3 endPos = projectileData.getPosition();
        Entity raycastEntity = projectileData.getEntity();

        BlockHitResult hitResult = level.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, raycastEntity));
        if (hitResult.getType() == HitResult.Type.BLOCK) {      // block hit
            BlockPos hitResultBlockPos = hitResult.getBlockPos();
            if (config.highlightLandingBlock) {
                blockMap.computeIfAbsent(hitResultBlockPos,
                        k -> new RenderBox(new AABB(hitResultBlockPos)).addOutlineColor(ColorUtils.getHighlightBlockColor(config, lineSource)));
            }
            line.addVertex(startPos, hitResult.getLocation(), ColorUtils.getLineColor(config, projectileData.getCharge(), lineSource));
            return OperationResult.RETURN;
        } else if (!config.useComplexPhysics) {            // if complex water physics are turned off, handle simple water physics
            BlockHitResult liquidHit = level.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, raycastEntity));
            projectileData.setInFluid(liquidHit.getType() != HitResult.Type.MISS);
        }
        return OperationResult.CONTINUE;
    }

    /**
     * Ticks down the projectile entity's lifetime (fireworks have a limited lifetime)
     *
     * @return if it has no lifetime left, stop simulating trajectory
     */
    private OperationResult applyLifetimeUpdate() {
        projectileData.subtractTick();
        if (projectileData.getTicksLeft() > 0) {
            return OperationResult.CONTINUE;
        }
        return OperationResult.BREAK;
    }
}
