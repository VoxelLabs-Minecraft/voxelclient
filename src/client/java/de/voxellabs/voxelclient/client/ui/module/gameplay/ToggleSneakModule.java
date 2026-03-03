package de.voxellabs.voxelclient.client.ui.module.gameplay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Toggle Sneak — VoxelClient v0.0.2
 * Hält den Sneak-Status aktiv, bis die Taste erneut gedrückt wird.
 */
public class ToggleSneakModule {

    private static KeyBinding toggleKey;
    private static boolean sneakEnabled = false;

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voxelclient.toggle_sneak",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "voxelclient.key.category"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                sneakEnabled = !sneakEnabled;
            }

            if (client.player == null) return;

            // Deaktiviere, wenn normaler Sneak-Key gedrückt und Toggle aktiv
            if (sneakEnabled) {
                client.options.sneakKey.setPressed(true);
            }
        });
    }

    public static boolean isEnabled() {
        return sneakEnabled;
    }

    public static void setEnabled(boolean enabled) {
        sneakEnabled = enabled;
    }
}
