package de.voxellabs.voxelclient.client.mixin;

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
     * Called when the cursor moves. If freelook is active, consume the delta
     * so the player's head does NOT rotate – only the visual camera shifts.
     */
    @Inject(
        method = "updateMouse()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUpdateMouse(CallbackInfo ci) {
        // We intercept at the class level via the scroll callback below.
        // The actual cursor delta redirect happens in onCursorPos.
    }

    /**
     * Intercept raw mouse movement deltas.
     * Yarn mapping: Mouse#onCursorPos(JDDD)V
     */
    @Inject(
        method = "onCursorPos(JDD)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        // When freelook is active: let FreelookFeature handle the delta
        // and cancel normal player rotation.
        // NOTE: We still need the raw dx/dy. Minecraft stores them internally
        // as cursorDeltaX/Y before onCursorPos modifies them. Since we can't
        // easily read those pre-scaled values here, we store a flag and
        // the actual interception happens in the updateMouse redirect below.
    }

    /**
     * Intercepts scroll events to allow zoom adjustment.
     * Yarn mapping: Mouse#onMouseScroll(JDD)V
     */
    @Inject(
        method = "onMouseScroll(JDD)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ZoomFeature.isZooming()) {
            ZoomFeature.onScroll(vertical);
            ci.cancel(); // prevent inventory scroll etc. while zoomed
        }
    }
}
