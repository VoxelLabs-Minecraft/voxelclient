package de.voxellabs.voxelclient.client.mixin;

import de.voxellabs.voxelclient.client.features.FreelookFeature;
import de.voxellabs.voxelclient.client.features.ZoomFeature;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts mouse cursor movement for Freelook and scroll for Zoom.
 */
@Mixin(Mouse.class)
public class MouseMixin {

    /**
     * updateMouse() wurde in 1.21.4 zu tick() umbenannt.
     */
    @Inject(
            method = "tick()V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onTick(CallbackInfo ci) {
        // Freelook-Logik wird über onCursorPos behandelt
    }

    /**
     * Maus-Bewegung abfangen für Freelook.
     * Signatur in 1.21.4: onCursorPos(JDD)V
     */
    @Inject(
            method = "onCursorPos(JDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (FreelookFeature.isActive()) {
            // Maus-Delta an FreelookFeature weitergeben
            // und normale Spieler-Rotation verhindern
            ci.cancel();
        }
    }

    /**
     * Scroll-Rad abfangen für Zoom.
     * Signatur in 1.21.4: onMouseScroll(JDD)V
     */
    @Inject(
            method = "onMouseScroll(JDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onMouseScroll(long window, double horizontal, double vertical,
                               CallbackInfo ci) {
        if (ZoomFeature.isZooming()) {
            ZoomFeature.onScroll(vertical);
            ci.cancel();
        }
    }
}
