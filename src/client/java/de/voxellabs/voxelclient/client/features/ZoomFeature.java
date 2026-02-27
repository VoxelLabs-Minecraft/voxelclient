package de.voxellabs.voxelclient.client.features;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;

/**
 * Zoom feature – smoothly zooms the camera FOV while the zoom key is held.
 * The actual FOV modification is applied via GameRendererMixin#onGetFov.
 */
public class ZoomFeature {

    private static boolean zooming       = false;
    private static double  currentFov    = -1;     // -1 means not initialised yet
    private static double  targetFov     = -1;
    private static double  scrollOffset  = 0.0;    // adjusted by scroll wheel

    private static KeyBinding zoomKey;

    public static void init(KeyBinding key) {
        zoomKey = key;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean shouldZoom = zoomKey.isPressed();

            if (!shouldZoom) {
                // Reset scroll offset when zoom is released
                scrollOffset = 0.0;
            }

            zooming = shouldZoom;
        });
    }

    /** Called from GameRendererMixin to modify the FOV. */
    public static double applyZoom(double originalFov, float tickDelta) {
        VoxelClientConfig cfg = VoxelClientConfig.get();

        if (!zooming) {
            if (cfg.zoomSmoothZoom) {
                // Smoothly return to normal FOV
                if (currentFov < 0) currentFov = originalFov;
                currentFov = lerp(currentFov, originalFov, 0.3);
                if (Math.abs(currentFov - originalFov) < 0.05) {
                    currentFov = originalFov;
                }
                return currentFov;
            }
            currentFov = -1;
            return originalFov;
        }

        // Calculate target zoom FOV (adjusted by scroll)
        double zoomFovTarget = Math.max(1.0, cfg.zoomFov - scrollOffset * 5.0 * cfg.zoomScrollSensitivity);

        if (cfg.zoomSmoothZoom) {
            if (currentFov < 0) currentFov = originalFov;
            targetFov = zoomFovTarget;
            currentFov = lerp(currentFov, targetFov, 0.25);
            return currentFov;
        }

        return zoomFovTarget;
    }

    /** Allows the scroll wheel to fine-tune zoom level. */
    public static void onScroll(double amount) {
        if (zooming) {
            scrollOffset += amount;
        }
    }

    public static boolean isZooming() {
        return zooming;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
