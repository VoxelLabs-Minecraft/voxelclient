package de.voxellabs.voxelclient.client.ui.module.gameplay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Toggle Sprint — VoxelClient v0.0.2
 * Hält den Sprint-Status aktiv, bis der Spieler stoppt oder erneut die Taste drückt.
 */
public class ToggleSprintModule {

    private static KeyBinding toggleKey;
    private static boolean sprintEnabled = false;
    private static boolean wasMoving = false;

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voxelclient.toggle_sprint",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "voxelclient.key.category"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                sprintEnabled = !sprintEnabled;
            }

            if (client.player == null) return;

            boolean isMoving = client.player.input.movementForward > 0.0f;

            // Deaktiviere Toggle wenn Spieler aufhört sich zu bewegen
            if (wasMoving && !isMoving) {
                sprintEnabled = false;
            }

            wasMoving = isMoving;

            if (sprintEnabled && isMoving && !client.player.isSprinting()) {
                client.player.setSprinting(true);
            }
        });
    }

    public static boolean isEnabled() {
        return sprintEnabled;
    }

    public static void setEnabled(boolean enabled) {
        sprintEnabled = enabled;
    }
}
