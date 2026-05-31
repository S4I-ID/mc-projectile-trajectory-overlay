package s4i.pto.services;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s4i.pto.model.LineSource;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.model.simulation.PredictionResult;
import s4i.pto.services.tasks.PredictionTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static s4i.pto.model.Constants.MOD_ID;

public class PredictionService {
    private static final Logger log = LoggerFactory.getLogger(MOD_ID);
    private final ExecutorService threadPool = Executors.newFixedThreadPool(6);
    private static PredictionService instance;

    private PredictionService() {}

    public static PredictionService getInstance() {
        if (instance == null) {
            instance = new PredictionService();
        }
        return instance;
    }

    public PredictionResult calculateTrajectoryLines(List<ProjectileData> projectileList, Player firingEntity, LineSource lineSource, Minecraft client) {
        PredictionResult predictionResult = new PredictionResult();
        float tickProgress = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Level world = client.level;
        Set<Future<PredictionResult>> results = new HashSet<>();

        projectileList.forEach(projectile ->
                results.add(threadPool.submit(new PredictionTask(projectile, firingEntity, lineSource, world, tickProgress))));
        results.forEach(future -> {
            try {
                predictionResult.merge(future.get());
            } catch (Exception e) {
                log.error("Failed to get prediction result", e);
            }
        });
        return predictionResult;
    }

}
