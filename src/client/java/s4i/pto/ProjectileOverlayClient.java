package s4i.pto;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s4i.pto.config.ModConfig;
import s4i.pto.config.ModKeyMappings;
import s4i.pto.model.LineSource;
import s4i.pto.services.OrchestratorService;
import s4i.pto.utils.ModUtils;

import java.util.Collection;
import java.util.stream.Stream;

import static s4i.pto.model.Constants.MOD_ID;

public class ProjectileOverlayClient implements ClientModInitializer {
    private static final Logger log = LoggerFactory.getLogger(MOD_ID);
    private static final Minecraft client = Minecraft.getInstance();

    /**
     * Use the Fabric API to bind our mod logic to internal game events
     */
    @Override
    public void onInitializeClient() {
        ModConfig config = ModConfig.getInstance();

        OrchestratorService orchestrator = OrchestratorService.getInstance();

        LevelRenderEvents.BEFORE_GIZMOS.register(context -> projectileOverlayEvent(context, orchestrator, config));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ModKeyMappings.PROJECTILE_OVERLAY_TOGGLE.consumeClick()) {
                config.toggleRenderProjectileLine();
            }
        });

        log.debug("S4I's Projectile Trajectory Overlay is loaded! :D");
    }

    /**
     * <p> This mod's logic, which is called during every {@link LevelRenderEvents#BEFORE_GIZMOS world render event seen above} </p>
     * @param context      used to get information about the world such as blocks and states and rendering utils
     * @param orchestrator this mod's main logic service class
     * @param config       this mod's configuration data (the options you see with ModMenu)
     */
    private void projectileOverlayEvent(LevelRenderContext context, OrchestratorService orchestrator, ModConfig config) {
        try {
            Player player = client.player;
            if (config.showProjectileTrajectoryForSelf && player != null) {
                orchestrator.resolveEntityTrajectoryPrediction(context, player, LineSource.SELF);
            }
            if (config.showProjectileTrajectoriesFromOtherPlayers && player != null) {
                Stream.ofNullable(client.level.getEntities(player, new AABB(player.getOnPos()).inflate(config.searchRadius)))
                        .flatMap(Collection::stream)
                        .filter(entity -> entity.isAlive() && !entity.isSpectator() && ModUtils.isClassOrSuperClass(entity, Player.class))
                        .forEach(entity -> {
                            Player foundPlayer = (Player) entity;
                            if (foundPlayer != null) {
                                orchestrator.resolveEntityTrajectoryPrediction(context, foundPlayer, LineSource.PLAYER);
                            }
                        });
            }
            if (config.showAlreadyFiredProjectileTrajectories && player != null) {
                orchestrator.resolveFiredProjectilesTrajectoryPrediction(context, LineSource.PROJECTILE);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Checks if game client is in singleplayer world or not
     */
    private static boolean isClientOnSingleplayerWorld() {
        return client.isSingleplayer();
    }
}