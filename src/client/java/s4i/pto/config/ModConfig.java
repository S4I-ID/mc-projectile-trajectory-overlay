package s4i.pto.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static s4i.pto.model.Constants.MOD_ID;

public class ModConfig {
    private static ModConfig instance;
    private static final Logger log = LoggerFactory.getLogger(MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    public int toggleKey = GLFW.GLFW_KEY_KP_1;
    public boolean showTrajectoryPredictionLine = true;
    public float lineWidth = 2.0f;
    public float lineWidthScaling = 1.0f;
    public int lineColor = 0x33ff00;
    public float lineColorA = 1.0f;
    public boolean hideEstimationsIfWeaponHasNoCharge = true;
    public boolean showDifferentTrajectoryColorAtMaxPower = false;
    public boolean showTrajectoryColorGradientDependingOnPower = false;
    public int maxDrawColor = 0x000000;
    public float maxDrawColorA = 1.0f;
    public float dottedLineScale = 1.0f;

    public boolean offsetTrajectorySelf = true;
    public float offsetMultiplier = 1.0f;
    public float offsetFalloffStrength = 0.35f;
    public float offsetSmoothnessMultiplier = 1.35f;
    public boolean hideClosestPartOfTrajectory = false;

    public boolean highlightLandingPoint = true;
    public boolean highlightLandingBlock = true;
    public int highlightPointColor = 0xffffff;
    public int highlightBlockColor = 0xffffff;
    public boolean highlightTargetedEntity = true;
    public boolean highlightTargetedEntityEdges = true;
    public int highlightEntityColor = 0x33ff00;
    public int highlightHostileEntityColor = 0x8800ff;
    public float highlightEntityColorA = 0.4f;

    public boolean useComplexPhysics = true;
    public int maxNumberOfTicksToSimulate = 200;
    public boolean showProjectileTrajectoryForSelf = true;
    public boolean showProjectileTrajectoriesFromOtherPlayers = true;
    public boolean showAlreadyFiredProjectileTrajectories = true;

    public int searchRadius = 250;
    public int alternativePlayerLineColor = 0xffff00;
    public float alternativePlayerLineColorA = 1.0f;
    public int alternativeProjectileLineColor = 0xffbb00;
    public float alternativeProjectileLineColorA = 1.0f;
    public int alternativeHighlightPointColor = 0xffff99;
    public float alternativeHighlightPointColorA = 1.0f;
    public int alternativeHighlightEntityColor = 0xffbb00;
    public float alternativeHighlightEntityColorA = 0.3f;
    public int alternativeHighlightBlockColor = 0xffbb00;

    public int numberOfDeviationMarkers = 0;
    public boolean showExplosionRadius = false;
    public int deviationColor = 0xff0000;
    public float deviationColorA = 0.15f;

    private ModConfig() {}

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = loadOrCreate();
            ModKeyMappings.registerKeys(instance);
        }
        return instance;
    }

    private static ModConfig loadOrCreate() {
        configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("s4i-projectile-trajectory-overlay.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (Exception exception) {
                log.error("Failed to read config file: {}", exception.getMessage());
                ModConfig defaultConfig = new ModConfig();
                delete();
                defaultConfig.save();
                return defaultConfig;
            }
        } else {
            log.warn("Config file not found - loading default config instead");
            ModConfig defaultConfig = new ModConfig();
            defaultConfig.save();
            return defaultConfig;
        }
    }

    private void save() {
        try {
            if (Files.exists(configPath)) {
                delete();
            }
            Files.createFile(configPath);
            Files.writeString(configPath, GSON.toJson(this), StandardOpenOption.WRITE);
        } catch (IOException exception) {
            log.error("Failed to write config file", exception);
            exception.fillInStackTrace();
        }
    }

    private static void delete() {
        try {
            Files.delete(configPath);
        } catch (IOException exception) {
            log.error("Failed to rewrite config file", exception);
            exception.fillInStackTrace();
        }
    }

    protected static void triggerSave() {
        instance.save();
        ModKeyMappings.updateKeybinds(instance);
    }

    public void toggleRenderProjectileLine() {
        instance.showTrajectoryPredictionLine = !instance.showTrajectoryPredictionLine;
    }
}
