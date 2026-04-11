package s4i.pto.config;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class ModKeyMappings {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("category.s4i-projectile-trajetory-overlay"));
    public static KeyBinding PROJECTILE_OVERLAY_TOGGLE;

    public static void registerKeys(ModConfig config) {
        PROJECTILE_OVERLAY_TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.projectile-trajectory-overlay.on-off", config.toggleKey, CATEGORY
        ));
    }

    public static void updateKeybinds(ModConfig config) {
        if (config.toggleKey != ModKeyMappings.PROJECTILE_OVERLAY_TOGGLE.getDefaultKey().getCode()) {
            PROJECTILE_OVERLAY_TOGGLE.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(config.toggleKey));
        }
    }
}
