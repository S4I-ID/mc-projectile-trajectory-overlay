package s4i.pto.services;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import s4i.pto.config.ModConfig;
import s4i.pto.model.LineSource;
import s4i.pto.model.projectile.ItemData;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.model.simulation.DeviationBox;
import s4i.pto.model.simulation.Line;
import s4i.pto.model.simulation.LineSegment;
import s4i.pto.model.simulation.PredictionResult;
import s4i.pto.model.simulation.RenderBox;
import s4i.pto.renderer.CustomRenderPipeline;
import s4i.pto.utils.ColorUtils;
import s4i.pto.utils.ModUtils;
import s4i.pto.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrchestratorService {
    private static OrchestratorService instance;

    private Minecraft client;
    private ItemRegistryService itemRegistryService;
    private EntityRegistryService entityRegistryService;
    private CustomRenderPipeline renderPipeline;
    private PredictionService predictionService;
    private ModConfig config;


    private OrchestratorService() {
    }

    public static OrchestratorService getInstance() {
        if (instance == null) {
            instance = new OrchestratorService();
            instance.itemRegistryService = ItemRegistryService.getInstance();
            instance.entityRegistryService = EntityRegistryService.getInstance();
            instance.renderPipeline = CustomRenderPipeline.getInstance();
            instance.client = Minecraft.getInstance();
            instance.config = ModConfig.getInstance();
            instance.predictionService = PredictionService.getInstance();
        }
        return instance;
    }

    public void resolveEntityTrajectoryPrediction(LevelRenderContext context, Player firingEntity, LineSource lineSource) {
        float tickProgress = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Player player = client.player;

        ItemData heldItemData = itemRegistryService.getProjectileItemClassIfHeldByEntity(firingEntity);
        if (heldItemData == null) {
            return;
        }
        if (!itemRegistryService.shouldDisplayLine(firingEntity, heldItemData.getItemClass())) {
            return;
        }
        List<ProjectileData> projectileList = itemRegistryService.getProjectileDataFromHeldItem(tickProgress, firingEntity, heldItemData);
        if (projectileList == null || projectileList.isEmpty()) {
            return;
        }
        if (config.hideEstimationsIfWeaponHasNoCharge && projectileList.getFirst().getCharge() <= 0f) {
            return;
        }
        Map<Integer, List<Line>> deviationLineMap = getDeviationLineMap(firingEntity, projectileList);
        PredictionResult simResult = predictionService.calculateTrajectoryLines(projectileList, firingEntity, lineSource, client);
        Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
        Vec3 handOffset = config.offsetTrajectorySelf ?
                itemRegistryService.getFirstPersonCameraOffset(firingEntity, player == firingEntity, heldItemData) : Vec3.ZERO;
        List<RenderBox> renderBoxes = new ArrayList<>(simResult.getEntityMap().values());
        renderBoxes.addAll(simResult.getBlockMap().values());

        renderLines(context, simResult.getLines(), cameraPos, handOffset);
        renderBoxes(context, renderBoxes, cameraPos);
        renderDeviationMarkers(context, deviationLineMap, cameraPos, player.getViewVector(tickProgress));
    }

    private Map<Integer, List<Line>> getDeviationLineMap(Player firingEntity, List<ProjectileData> projectileList) {
        if (config.numberOfDeviationMarkers > 0) {
            List<ProjectileData> list = new ArrayList<>();
            for (int i = 0; i < projectileList.size(); i++) {
                ProjectileData projectileData = projectileList.get(i);
                list.add(new ProjectileData(projectileData, i, false));
                list.add(new ProjectileData(projectileData, i, true));
            }
            PredictionResult deviationResult = predictionService.calculateTrajectoryLines(list, firingEntity, null, client);
            return deviationResult.getLines().stream()
                    .collect(Collectors.groupingBy(Line::getDeviationId));
        }
        return null;
    }

    public void resolveFiredProjectilesTrajectoryPrediction(LevelRenderContext context, LineSource lineSource) {
        Player player = client.player;

        List<ProjectileData> projectileList = Stream.ofNullable(client.level
                        .getEntities(player, new AABB(player.getOnPos()).inflate(config.searchRadius)))
                .flatMap(Collection::stream)
                .filter(entity -> entity != null && entity.isAlive() && !entity.isSpectator() && ModUtils.isClassOrSuperClass(entity, Projectile.class))
                .map(entity -> entityRegistryService.getProjectileData(entity))
                .filter(Objects::nonNull)
                .filter(projectileData -> projectileData.getCharge() > 0f)
                .collect(Collectors.toCollection(ArrayList::new));

        if (projectileList.isEmpty()) {
            return;
        }
        PredictionResult simResult = predictionService.calculateTrajectoryLines(projectileList, null, lineSource, client);
        Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
        List<RenderBox> renderBoxes = new ArrayList<>(simResult.getEntityMap().values());
        renderBoxes.addAll(simResult.getBlockMap().values());
        renderLines(context, simResult.getLines(), cameraPos, Vec3.ZERO);
        renderBoxes(context, renderBoxes, cameraPos);
    }

    private void renderLines(LevelRenderContext context, List<Line> lines, Vec3 cameraPos, Vec3 handOffset) {
        if (config.showTrajectoryPredictionLine && !lines.isEmpty()) {
            if (config.hideClosestPartOfTrajectory) {
                lines.forEach(Line::removeFirstVertex);
            }
            renderPipeline.renderLines(context, lines, handOffset.multiply(1, 0, 1), cameraPos);
        }
    }

    private void renderBoxes(LevelRenderContext context, List<RenderBox> boxes, Vec3 cameraPos) {
        if (!boxes.isEmpty()) {
            renderPipeline.renderBoxes(context, boxes, cameraPos);
        }
    }

    private void renderDeviationMarkers(LevelRenderContext context, Map<Integer, List<Line>> lineMap, Vec3 cameraPos, Vec3 playerRotationVector) {
        if (lineMap == null || lineMap.isEmpty()) {
            return;
        }
        List<DeviationBox> boxes = new ArrayList<>();
        lineMap.forEach((key, deviationPair) -> {
            Line minDeviationLine = deviationPair.getFirst();
            Line maxDeviationLine = deviationPair.getLast();
            List<LineSegment> minDeviationSegments = minDeviationLine.getSegments();
            List<LineSegment> maxDeviationSegments = maxDeviationLine.getSegments();
            int minSize = minDeviationSegments.size() - 1;
            int maxSize = maxDeviationSegments.size() - 1;
            for (int i = 0; i < config.numberOfDeviationMarkers; i++) {
                boxes.add(new DeviationBox(
                        minDeviationSegments.get(Math.max(0, Mth.ceil(minSize * (1 - i)))).end,
                        maxDeviationSegments.get(Math.max(0, Mth.ceil(maxSize * (1 - i)))).end,
                        i, ColorUtils.getDeviationColor(config, LineSource.SELF)
                ));
            }
        });
        boolean facingAway = (Utils.doubleBetween(playerRotationVector.x, 0, 1) && Utils.doubleBetween(playerRotationVector.z, -1, 0)) ||
                (Utils.doubleBetween(playerRotationVector.x, -1, 0) && Utils.doubleBetween(playerRotationVector.z, 0, 1));
        if (!boxes.isEmpty()) {
            renderPipeline.renderDeviationMarkers(context, boxes, cameraPos, facingAway);
        }
    }
}
