package s4i.pto;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s4i.pto.config.ModConfig;
import s4i.pto.config.ModKeyMappings;
import s4i.pto.model.LineSource;
import s4i.pto.services.OrchestratorService;

import java.util.Collection;
import java.util.stream.Stream;

import static s4i.pto.model.Constants.MOD_ID;

public class ProjectileOverlayClient implements ClientModInitializer {
    private static final Logger log = LoggerFactory.getLogger(MOD_ID);
    private static final MinecraftClient client = MinecraftClient.getInstance();

    /**
     * Use the Fabric API to bind our mod logic to internal game events
     */
    @Override
    public void onInitializeClient() {
        ModConfig config = ModConfig.getInstance();

        OrchestratorService orchestrator = OrchestratorService.getInstance();

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            try {
                projectileOverlayEvent(context, orchestrator, config);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ModKeyMappings.PROJECTILE_OVERLAY_TOGGLE.wasPressed()) {
                config.toggleRenderProjectileLine();
            }
        });
        log.debug("S4I's Projectile Trajectory Overlay is loaded! :D");
    }

    /**
     * <p> This mod's logic, which is called during every {@link WorldRenderEvents#BEFORE_DEBUG_RENDER world render event seen above} </p>
     * @param context      used to get information about the world such as blocks and states and rendering utils
     * @param orchestrator this mod's main logic service class
     * @param config       this mod's configuration data (the options you see with ModMenu)
     */
    private void projectileOverlayEvent(WorldRenderContext context, OrchestratorService orchestrator, ModConfig config) {
        PlayerEntity player = client.player;
        if (config.showProjectileTrajectoryForSelf && player != null) {
            orchestrator.resolveEntityTrajectoryPrediction(context, player, LineSource.SELF);
        }
        if (config.showProjectileTrajectoriesFromOtherPlayers && player != null) {
            Stream.ofNullable(client.world.getOtherEntities(player, new Box(player.getBlockPos()).expand(config.searchRadius)))
                    .flatMap(Collection::stream)
                    .filter(entity -> entity.isAlive() && !entity.isSpectator() && entity.isPlayer())
                    .forEach(entity -> {
                        PlayerEntity foundPlayer = (PlayerEntity) entity.getEntity();
                        if (foundPlayer != null) {
                            orchestrator.resolveEntityTrajectoryPrediction(context, foundPlayer, LineSource.PLAYER);
                        }
                    });
        }
        if (config.showAlreadyFiredProjectileTrajectories && player != null) {
            orchestrator.resolveFiredProjectilesTrajectoryPrediction(context, LineSource.PROJECTILE);
        }
    }
}