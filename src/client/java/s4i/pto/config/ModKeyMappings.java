package s4i.pto.config;


import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import s4i.pto.model.Constants;

public class ModKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier
            .fromNamespaceAndPath(Constants.MOD_ID, "category.s4i-projectile-trajetory-overlay"));
    public static KeyMapping PROJECTILE_OVERLAY_TOGGLE;

    public static void registerKeys(ModConfig config) {
        PROJECTILE_OVERLAY_TOGGLE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "Projectile Trajectory Overlay On/Off",
                InputConstants.Type.KEYSYM,
                config.toggleKey,
                CATEGORY
        ));
    }

    public static void updateKeybinds(ModConfig config) {
        if (config.toggleKey != ModKeyMappings.PROJECTILE_OVERLAY_TOGGLE.getDefaultKey().getValue()) {
            PROJECTILE_OVERLAY_TOGGLE.setKey(InputConstants.Type.KEYSYM.getOrCreate(config.toggleKey));
        }
    }
}
