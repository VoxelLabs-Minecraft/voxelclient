package de.voxellabs.voxelclient.client.features;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;

/**
 * Freelook feature – lets the player look around without turning the body.
 *
 * While the freelook key is held:
 *   • Mouse delta is stored in yawOffset / pitchOffset instead of rotating the player.
 *   • CameraSubmersionType is unaffected; only visual yaw/pitch are shifted.
 *   • MouseMixin reads these offsets to move the camera independently.
 *
 * This is a "hold to activate" approach (like Optifine's freelook).
 */
public class FreelookFeature {

    private static boolean active     = false;
    private static float   yawOffset  = 0f;
    private static float   pitchOffset = 0f;

    // Saved player rotation at the moment freelook was activated
    private static float   savedPlayerYaw   = 0f;
    private static float   savedPlayerPitch = 0f;

    private static KeyBinding freelookKey;

    public static void init(KeyBinding key) {
        freelookKey = key;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean shouldActivate = freelookKey.isPressed() && VoxelClientConfig.get().freelookEnabled;

            if (shouldActivate && !active) {
                // Just activated – save current rotation
                if (client.player != null) {
                    savedPlayerYaw   = client.player.getYaw();
                    savedPlayerPitch = client.player.getPitch();
                }
                yawOffset   = 0f;
                pitchOffset = 0f;
            } else if (!shouldActivate && active) {
                // Just deactivated – reset offsets
                yawOffset   = 0f;
                pitchOffset = 0f;
            }

            active = shouldActivate;
        });
    }

    /** Called from MouseMixin instead of rotating the player. */
    public static boolean onMouseMoved(double dx, double dy) {
        if (!active) return false;

        VoxelClientConfig cfg = VoxelClientConfig.get();
        float sens = cfg.freelookSensitivity;

        yawOffset   += (float) dx * 0.15f * sens;
        pitchOffset += (float) dy * 0.15f * sens;

        // Clamp pitch so you can't look past straight up/down
        pitchOffset = Math.max(-90f, Math.min(90f, pitchOffset));

        return true; // consumed – don't rotate the player
    }

    public static boolean isActive()       { return active; }
    public static float   getYawOffset()   { return yawOffset; }
    public static float   getPitchOffset() { return pitchOffset; }
}
