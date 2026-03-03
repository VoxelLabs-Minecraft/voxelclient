package de.voxellabs.voxelclient.client.ui.module.gameplay;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

/**
 * Snap Look — VoxelClient v0.0.2
 * Rastert die Kamera auf den nächsten 45°-Schritt (N, NE, E, SE, S, SW, W, NW).
 * Nützlich für Screenshots, Karten und präzise Ausrichtung.
 */
public class SnapLookModule {

    private static KeyBinding snapKey;

    // Erlaubte Yaw-Winkel (in Minecraft-Grad: 0=S, 90=W, 180=N, 270=E)
    private static final float[] SNAP_ANGLES = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};

    public static void register() {
        snapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voxelclient.snap_look",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "voxelclient.key.category"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!snapKey.wasPressed()) return;
            if (client.player == null) return;

            float currentYaw = client.player.getYaw();
            float normalized = ((currentYaw % 360f) + 360f) % 360f;

            float closest = SNAP_ANGLES[0];
            float minDiff = angleDiff(normalized, SNAP_ANGLES[0]);

            for (float angle : SNAP_ANGLES) {
                float diff = angleDiff(normalized, angle);
                if (diff < minDiff) {
                    minDiff = diff;
                    closest = angle;
                }
            }

            client.player.setYaw(closest);
            // Pitch auf 0 (Horizont) snappen, optional
            // client.player.setPitch(0f);
        });
    }

    private static float angleDiff(float a, float b) {
        float diff = Math.abs(a - b) % 360f;
        return diff > 180f ? 360f - diff : diff;
    }
}
