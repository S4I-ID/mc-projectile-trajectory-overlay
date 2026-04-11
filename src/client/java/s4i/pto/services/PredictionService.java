package s4i.pto.services;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PredictionService {
    /**
     * Some projectiles do not use their overriden on bubble column collision function and use the {@link Entity#onBubbleColumnCollision base class} one instead
     */
    private static final Set<Class<?>> cappedBubbleDragItems = Set.of(EnderPearlItem.class);
    private static final Set<Class<?>> explosiveProjectileEntities = Set.of(FireworkRocketEntity.class);
    private static final List<Class<?>> entitiesWithCollision = List.of(LivingEntity.class, BlockAttachedEntity.class, VehicleEntity.class);
    private static PredictionService instance;
    private static ModConfig config;

    private PredictionService() {}

    public static PredictionService getInstance() {
        if (instance == null) {
            instance = new PredictionService();
            instance.init();
        }
        return instance;
    }

    private void init() {
        config = ModConfig.getInstance();
    }

    // TODO test performance with virtual threads after upgrading to Java 25
    public PredictionResult calculateTrajectoryLines(List<ProjectileData> projectileList, PlayerEntity firingEntity, LineSource lineSource, MinecraftClient client) {
        PredictionResult predictionResult = new PredictionResult();
        float tickProgress = client.getRenderTickCounter().getTickProgress(false);

        projectileList.forEach(projectile -> {
            ClientWorld world = client.world;
            calculateTrajectoryLine(predictionResult, projectile, firingEntity, lineSource, world, tickProgress);
            if (projectile.isExplosive() && config.showExplosionRadius) {
                simulateExplosion(predictionResult, projectile, world, tickProgress, lineSource);
            }
        });
        return predictionResult;
    }

    /**
     * Simulates explosion at the end of the projectile. Used for fireworks.
     * @see FireworkRocketEntity#explode(ServerWorld)
     */
    private void simulateExplosion(PredictionResult predictionResult, ProjectileData projectile, ClientWorld world, float tickProgress, LineSource lineSource) {
        if (explosiveProjectileEntities.contains(projectile.getEntity().getClass())) {
            Vec3d lastPosition = projectile.getLastPosition();
            if (projectile.getExplosions() <= 0) {
                return;
            }

            Box explosionBox = projectile.getEntity().getBoundingBox().expand(Constants.FIREWORK_EXPLOSION_RANGE);
            predictionResult.entityMap.computeIfAbsent(new ArrowEntity(EntityType.ARROW, world), k -> new RenderBox(explosionBox,
                    ColorUtils.getDeviationColor(config, lineSource), ColorUtils.getDeviationColor(config, lineSource)));

            for (LivingEntity livingEntity : world.getNonSpectatingEntities(LivingEntity.class, explosionBox)) {
                if (lastPosition.squaredDistanceTo(livingEntity.getEntityPos()) <= Constants.FIREWORK_EXPLOSION_RANGE * Constants.FIREWORK_EXPLOSION_RANGE) {
                    boolean hitsEntity = false;

                    for (int i = 0; i < 2; i++) {
                        Vec3d entityCheckPos = new Vec3d(livingEntity.getX(), livingEntity.getBodyY(0.5 * i), livingEntity.getZ());
                        HitResult hitResult = world.raycast(
                                new RaycastContext(lastPosition, entityCheckPos, RaycastContext.ShapeType.COLLIDER,
                                        RaycastContext.FluidHandling.NONE, projectile.getEntity()));
                        if (hitResult.getType() == HitResult.Type.MISS) {
                            hitsEntity = true;
                            break;
                        }
                    }
                    if (hitsEntity) {
                        boolean isHostile = EntityUtils.isEntityHostile(livingEntity);
                        predictionResult.entityMap.computeIfAbsent(livingEntity, k -> new RenderBox(EntityUtils.getLerpedBoundingBox(k, tickProgress),
                                config.highlightTargetedEntity ? ColorUtils.getHighlightEntityColor(config, lineSource, isHostile) : null,
                                config.highlightTargetedEntityEdges ? ColorUtils.getHighlightEntityOutlineColor(config, lineSource, isHostile) : null));
                    }
                }
            }
        }
    }

    private void calculateTrajectoryLine(PredictionResult predictionResult, ProjectileData projectileData, PlayerEntity firingEntity, LineSource lineSource,
                                         ClientWorld world, float tickProgress) {
        Line line = new Line(projectileData.getDeviationId(), lineSource);
        List<LineSegment> vertices = line.getSegments();
        predictionResult.lines.add(line);

        boolean isDeviation = projectileData.isDeviation();     // if is deviation, discard boxes
        Map<Entity, RenderBox> entityMap = !isDeviation ? predictionResult.entityMap : new HashMap<>();
        Map<BlockPos, RenderBox> blockMap = !isDeviation ? predictionResult.blockMap : new HashMap<>();

        Entity raycastEntity = projectileData.getEntity();
        raycastEntity.setPosition(projectileData.getPosition());
        raycastEntity.setYaw(projectileData.getYaw());
        raycastEntity.setPitch(projectileData.getPitch());

        for (int tick = 0; tick < config.maxNumberOfTicksToSimulate; tick++) {
            for (Operation operation : projectileData.getOperationOrder()) {
                OperationResult result = switch (operation) {
                    case DRAG -> applyDrag(projectileData);
                    case VELOCITY -> applyVelocity(projectileData);
                    case POSITION -> applyPosition(projectileData);
                    case INITIAL_THROWN_BUBBLE_COLLISION -> applyInitialThrownEntityBubbleColumnCollision(projectileData, tick);
                    case BLOCK_COLLISION -> applyBlockCollisions(projectileData, world);
                    case ROTATION -> applyRotation(projectileData);
                    case FISHING_BOBBER -> applyFishingBobberBuoyancy(projectileData);
                    case UPDATE_WATER_STATE -> applyFluidVelocity(projectileData, world);
                    case ENTITY_COLLISION -> applyEntityCollision(projectileData, world, firingEntity, tickProgress, entityMap, line, lineSource);
                    case BLOCK_RAYCAST -> applyBlockRaycast(projectileData, world, blockMap, line, lineSource);
                    case LIFETIME_UPDATE -> applyLifetimeUpdate(projectileData);
                    case null, default -> throw new UnsupportedOperationException(String.format("Unknown estimation case: %s", operation));
                };
                if (result == OperationResult.RETURN) {
                    return;
                }
                if (result == OperationResult.BREAK) {
                    tick = config.maxNumberOfTicksToSimulate;
                }
            }
            line.addVertex(projectileData.getLastPosition(), projectileData.getPosition(),
                    ColorUtils.getLineColor(config, projectileData.getCharge(), lineSource));
        }

        if (config.highlightLandingBlock && projectileData.isForceHighlightBlock() && !vertices.isEmpty()) {
            Vec3d lastPos = vertices.getLast().end;
            BlockPos blockPos = new BlockPos(MathHelper.floor(lastPos.x), MathHelper.floor(lastPos.y), MathHelper.floor(lastPos.z));
            blockMap.computeIfAbsent(blockPos,
                    k -> new RenderBox(new Box(blockPos)).addOutlineColor(ColorUtils.getHighlightBlockColor(config, lineSource)));
        }
    }


    /**
     * Applies the drag operation depending on the liquid submersion state of the entity
     */
    private static OperationResult applyDrag(ProjectileData projectileData) {
        float drag = projectileData.isInFluid() ? projectileData.getWaterDrag() : projectileData.getAirDrag();
        projectileData.setVelocity(projectileData.getVelocity().multiply(drag));
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Adds updates to projectile's velocity, usually gravitational force  </p>
     * <p> Fishing bobbers use a different movement function </p>
     * @see FishingBobberEntity#tick()
     */
    private static OperationResult applyVelocity(ProjectileData projectileData) {
        if (projectileData.getItemClassFiredFrom().equals(FishingRodItem.class)) {
            applyFishingBobberMovement(projectileData);
        } else {
            projectileData.setVelocity(projectileData.getVelocity().add(projectileData.getGravityVelocity()));
        }
        return OperationResult.CONTINUE;
    }

    /**
     * Fishing bobber uses a certain part of the underlying Entity class for velocity calculation
     * @see net.minecraft.item.TridentItem#onStoppedUsing
     * @see net.minecraft.entity.projectile.FishingBobberEntity#tick()
     * @see Entity#move
     * @see Entity#getVelocityAffectingPos()
     */
    private static void applyFishingBobberMovement(ProjectileData projectileData) {
        World world = projectileData.getEntity().getEntityWorld();
        Vec3d pos = projectileData.getPosition();
        BlockPos blockPos = new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y), MathHelper.floor(pos.z));
        BlockState blockState = world.getBlockState(blockPos);
        float f = blockState.getBlock().getVelocityMultiplier();
        float x;
        if (!blockState.isOf(Blocks.WATER) && !blockState.isOf(Blocks.BUBBLE_COLUMN)) {
            x = f == 1.0f ?
                    world.getBlockState(new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y - 0.500001f), MathHelper.floor(pos.z)))
                    .getBlock().getVelocityMultiplier()
                    : f;
        } else {
            x = f;
        }
        projectileData.setVelocity(projectileData.getVelocity().multiply(x, 1.0, x));
    }

    /**
     * Updates entity position according to current velocity
     */
    private static OperationResult applyPosition(ProjectileData projectileData) {
        projectileData.setLastPosition(projectileData.getPosition());
        projectileData.addToPosition(projectileData.getVelocity());
        projectileData.getEntity().setPosition(projectileData.getPosition());
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Thrown items get this check at the beginning of their tick  </p>
     * @see ThrownEntity#tick()
     * @see ThrownEntity#tickInitialBubbleColumnCollision()
     */
    private static OperationResult applyInitialThrownEntityBubbleColumnCollision(ProjectileData projectileData, int tick) {
        if (tick == 0 && config.useComplexPhysics) {
            Entity raycastEntity = projectileData.getEntity();
            World world = raycastEntity.getEntityWorld();
            for (BlockPos blockPos : BlockPos.iterate(raycastEntity.getBoundingBox())) {
                BlockState blockState = world.getBlockState(blockPos);
                if (blockState.isOf(Blocks.BUBBLE_COLUMN)) {
                    BlockState airBlock = world.getBlockState(blockPos.up());
                    boolean isSurfaceBubbleColumn = airBlock.getCollisionShape(world, blockPos).isEmpty() && airBlock.getFluidState().isEmpty();
                    boolean isDownward = blockState.get(BubbleColumnBlock.DRAG);
                    double bubbleDrag = ProjectileUtils.getBubbleDragForProjectiles(isSurfaceBubbleColumn, isDownward);
                    projectileData.setVelocity(projectileData.getVelocity().add(0, bubbleDrag, 0));
                }
            }
        }
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Each block the entity intersects (even a little) will
     * {@link AbstractBlock#onEntityCollision(BlockState, World, BlockPos, Entity, EntityCollisionHandler, boolean) apply its effects} on the passing entity </p>
     * <p> Collision checker used for entities, rewritten to use our data model </p>
     * <p> Checks up to 16 blocks plus the final block between our entity's starting and ending position </p>
     * <p> Player entities use this method differently, see attached function </p>
     * @see Entity#checkBlockCollisions(List, EntityCollisionHandler.Impl)
     * @see Entity#tick()
     */
    private static OperationResult applyBlockCollisions(ProjectileData projectileData, World world) {
        if (config.useComplexPhysics) {
            Set<Long> visited = new HashSet<>(32);

            Vec3d from = Utils.copyVec3d(projectileData.getLastPosition());
            Vec3d to = projectileData.getPosition();
            int stepsLeft = 16;

            stepsLeft -= checkBlockCollision(projectileData, from, to, world, visited, 16);
            if (stepsLeft <= 0) {       // if entity is moving very fast, collision check won't reach final block; do final block check
                checkBlockCollision(projectileData, to, to, world, visited, 1);
            }
        }
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Modified block collision checker </p>
     * <p> We are only interested in blocks that affect our projectile's flight path: underwater bubble columns </p>
     * @see Entity#checkBlockCollision(Vec3d, Vec3d, EntityCollisionHandler.Impl, LongSet, int)
     */
    private static int checkBlockCollision(ProjectileData projectileData, Vec3d from, Vec3d to, World world, Set<Long> visited, int stepsLeft) {
        Entity raycastEntity = projectileData.getEntity();
        Box box = ((EntityAccessor) raycastEntity).getDimensions().getBoxAt(to).contract(0.00001f);
        AtomicInteger steps = new AtomicInteger();
        BlockView.collectCollisionsBetween(from, to, box, (BlockPos blockPos, int j) -> {
            if (j >= stepsLeft)
                return false;
            else {
                steps.set(j);
                BlockState blockState = world.getBlockState(blockPos);
                if (!blockState.isAir()) {
                    boolean bl4 = EntityUtils.entityCollidesWithFluid(blockState.getFluidState(), blockPos, from, to, world, raycastEntity);
                    if (bl4 && visited.add(blockPos.asLong())) {
                        if (blockState.isOf(Blocks.BUBBLE_COLUMN)) {
                            BlockPos airBlockPos = blockPos.up();
                            BlockState airBlock = world.getBlockState(airBlockPos);
                            boolean isSurfaceBubbleColumn = airBlock.getCollisionShape(world, airBlockPos).isEmpty() && airBlock.getFluidState().isEmpty();
                            applyBubbleColumn(projectileData, isSurfaceBubbleColumn, blockState.get(BubbleColumnBlock.DRAG));
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
     * @see Entity#onBubbleColumnCollision(boolean)
     * @see ProjectileEntity#onBubbleColumnCollision(boolean)
     */
    private static void applyBubbleColumn(ProjectileData projectileData, boolean isSurfaceColumn, boolean isDownward) {
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
     * @see ProjectileEntity#updateRotation(float, float)
     * @see ProjectileEntity#updateRotation()
     */
    private static OperationResult applyRotation(ProjectileData projectileData) {
        Vec3d currentVelocity = projectileData.getVelocity();
        float newYaw = (float) (MathHelper.atan2(currentVelocity.x, currentVelocity.z) * 180.0F / (float) Math.PI);
        float newPitch = (float) (MathHelper.atan2(currentVelocity.y, currentVelocity.horizontalLength()) * 180.0F / (float) Math.PI);
        float updatedYaw = ProjectileUtils.getNewEntityRotation(projectileData.getYaw(), newYaw);
        float updatedPitch = ProjectileUtils.getNewEntityRotation(projectileData.getPitch(), newPitch);
        projectileData.setYaw(updatedYaw);
        projectileData.setPitch(updatedPitch);
        Entity raycastEntity = projectileData.getEntity();
        raycastEntity.setYaw(updatedYaw);
        raycastEntity.setPitch(updatedPitch);
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Unique buoyancy used by fishing bobber </p>
     * @see FishingBobberEntity#tick()
     * @see FishingBobberEntity.State
     */
    private OperationResult applyFishingBobberBuoyancy(ProjectileData projectileData) {
        World world = projectileData.getEntity().getEntityWorld();
        Vec3d pos = projectileData.getPosition();
        BlockPos blockPos = new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y), MathHelper.floor(pos.z));

        if (projectileData.getState() == 0 && (projectileData.isInFluid() || !world.getFluidState(blockPos).isEmpty())) {
            projectileData.setState(-1);
            projectileData.setVelocity(projectileData.getVelocity().multiply(0.3f, 0.2f, 0.3f));
        } else if (projectileData.getState() != 0) {
            if (!projectileData.isInFluid()) {
                projectileData.setState(0);
            }
            Vec3d velocity = projectileData.getVelocity();

            FluidState fluidState = world.getFluidState(blockPos);
            double d = pos.y + velocity.y - blockPos.getY() - fluidState.getHeight(world, blockPos);
            if (Math.abs(d) < 0.01) {
                d += Math.signum(d) * 0.1;
            }
            projectileData.setVelocity(new Vec3d(velocity.x * 0.9f, velocity.y - d * 0.1f, velocity.z * 0.9f));
        }
        if (!projectileData.isInFluid()) {
            projectileData.setVelocity(projectileData.getVelocity().add(projectileData.getGravityVelocity()));
        }
        return OperationResult.CONTINUE;
    }

    /**
     * <p> Updates water state for our projectile. Adds weighted velocity of fluids to projectile. </p>
     * <p> Also sets the "isInFluid" flag. </p>
     * @see Entity#baseTick()
     * @see Entity#checkWaterState()
     * @see Entity#updateWaterState()
     * @see Entity#updateMovementInFluid(TagKey, double)
     */
    private static OperationResult applyFluidVelocity(ProjectileData projectileData, World world) {
        boolean isInWater = updateMovementInFluid(projectileData, world,FluidTags.WATER,Constants.WATER_SPEED);
        double lavaSpeed = world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.FAST_LAVA_GAMEPLAY) ?
                Constants.FAST_LAVA_SPEED : Constants.SLOW_LAVA_SPEED;
        boolean isInLava = updateMovementInFluid(projectileData, world, FluidTags.LAVA,lavaSpeed);
        projectileData.setIsInFluid(isInWater || isInLava);
        return OperationResult.CONTINUE;
    }

    /**
     *
     * @param fluidTag fluid tag to check
     * @param fluidSpeed fluid speed for velocity calculation
     * @return true if entity's bounding box collides with the given fluid
     * @see Entity#updateMovementInFluid(TagKey, double)
     */
    private static boolean updateMovementInFluid(ProjectileData projectileData, World world, TagKey<Fluid> fluidTag, double fluidSpeed) {
        Entity entity = projectileData.getEntity();
        if (entity.isRegionUnloaded()) {
            return false;
        } else {
            Box box = entity.getBoundingBox().contract(0.001);
            int i = MathHelper.floor(box.minX);
            int j = MathHelper.ceil(box.maxX);
            int k = MathHelper.floor(box.minY);
            int l = MathHelper.ceil(box.maxY);
            int m = MathHelper.floor(box.minZ);
            int n = MathHelper.ceil(box.maxZ);
            double waterHeightMultiplier = 0.0;
            boolean isPushedByFluids = entity.isPushedByFluids();
            boolean isInFluid = false;
            Vec3d totalFluidVelocity = Vec3d.ZERO;
            int fluidBlocks = 0;
            BlockPos.Mutable mutable = new BlockPos.Mutable();

            for (int p = i; p < j; p++) {
                for (int q = k; q < l; q++) {
                    for (int r = m; r < n; r++) {
                        mutable.set(p, q, r);
                        FluidState fluidState = world.getFluidState(mutable);
                        if (fluidState.isIn(fluidTag)) {
                            double waterBlockHeight = q + fluidState.getHeight(world, mutable);
                            if (waterBlockHeight >= box.minY) {
                                isInFluid = true;
                                waterHeightMultiplier = Math.max(waterBlockHeight - box.minY, waterHeightMultiplier);
                                if (isPushedByFluids) {
                                    Vec3d fluidVelocity = fluidState.getVelocity(world, mutable);
                                    if (waterHeightMultiplier < 0.4) {
                                        fluidVelocity = fluidVelocity.multiply(waterHeightMultiplier);
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
                    totalFluidVelocity = totalFluidVelocity.multiply(1.0 / fluidBlocks);
                }
                if (!projectileData.getEntity().isPlayer()) {
                    totalFluidVelocity = totalFluidVelocity.normalize();
                }
                Vec3d initialVelocity = projectileData.getVelocity();
                totalFluidVelocity = totalFluidVelocity.multiply(fluidSpeed);
                double f = 0.003;
                if (Math.abs(initialVelocity.x) < f && Math.abs(initialVelocity.z) < f && totalFluidVelocity.length() < 0.0045000000000000005) {
                    totalFluidVelocity = totalFluidVelocity.normalize().multiply(0.0045000000000000005);
                }
                projectileData.setVelocity(initialVelocity.add(totalFluidVelocity));
            }
            return isInFluid;
        }
    }

    /**
     * <p> Handles entity collisions. Certain projectiles pierce through multiple entities (crossbow bolts) </p>
     * @return stops the trajectory simulation if it cannot pierce any more entities
     */
    private static OperationResult applyEntityCollision(ProjectileData projectileData, ClientWorld world, PlayerEntity firingEntity, float tickProgress,
                                                        Map<Entity, RenderBox> entityMap, Line line, LineSource lineSource) {
        Vec3d startPos = projectileData.getLastPosition();
        Vec3d endPos = projectileData.getPosition();

        Box entityCheckBox = new Box(startPos, endPos).expand(1.0d);    // get entities near projectile position
        List<Entity> entities = world.getOtherEntities(firingEntity, entityCheckBox).stream()
                .filter(entity -> entity.isAlive() && ModUtils.isInClassesOrSuperClasses(entity, entitiesWithCollision))
                .sorted((e1, e2) -> EntityUtils.comparatorEntityPosToPoint(e1, e2, startPos))
                .collect(Collectors.toCollection(ArrayList::new));

        for (Entity entity : entities) {
            Box boundingBox = EntityUtils.getLerpedBoundingBox(entity, tickProgress);
            if (boundingBox.expand(entity.getTargetingMargin()).intersects(startPos, endPos)) {
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
     * @return stops the trajectory simulation if entity hitsEntity a solid block
     */
    private static OperationResult applyBlockRaycast(ProjectileData projectileData, World world, Map<BlockPos, RenderBox> blockMap, Line line, LineSource lineSource) {
        Vec3d startPos = projectileData.getLastPosition();
        Vec3d endPos = projectileData.getPosition();
        Entity raycastEntity = projectileData.getEntity();

        BlockHitResult hitResult = world.raycast(new RaycastContext(startPos, endPos,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, raycastEntity));

        if (hitResult.getType() == HitResult.Type.BLOCK) {      // block hit
            BlockPos hitResultBlockPos = hitResult.getBlockPos();
            if (config.highlightLandingBlock) {
                blockMap.computeIfAbsent(hitResultBlockPos,
                        k -> new RenderBox(new Box(hitResultBlockPos)).addOutlineColor(ColorUtils.getHighlightBlockColor(config, lineSource)));
            }
            line.addVertex(startPos, hitResult.getPos(), ColorUtils.getLineColor(config, projectileData.getCharge(), lineSource));
            return OperationResult.RETURN;
        } else if (!config.useComplexPhysics) {            // if complex water physics are turned off, handle simple water physics
            BlockHitResult liquidHit = world.raycast(new RaycastContext(startPos, endPos,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, raycastEntity));
            projectileData.setIsInFluid(liquidHit.getType() != HitResult.Type.MISS);
        }
        return OperationResult.CONTINUE;
    }

    /**
     * Ticks down the projectile entity's lifetime (fireworks have a limited lifetime)
     * @return if it has no lifetime left, stop simulating trajectory
     */
    private static OperationResult applyLifetimeUpdate(ProjectileData projectileData) {
        projectileData.subtractTick();
        if (projectileData.getTicksLeft() > 0) {
            return OperationResult.CONTINUE;
        }
        return OperationResult.BREAK;
    }
}
