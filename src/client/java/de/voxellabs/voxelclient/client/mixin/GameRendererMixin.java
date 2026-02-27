package de.voxellabs.voxelclient.client.mixin;

import de.voxellabs.voxelclient.client.features.ZoomFeature;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts FOV calculation to apply zoom and smooth transitions.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(
            method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov,
                          CallbackInfoReturnable<Float> cir) {
        float originalFov = cir.getReturnValue();
        double newFov = ZoomFeature.applyZoom(originalFov, tickDelta);
        if (newFov != originalFov) {
            cir.setReturnValue((float) newFov);
        }
    }
}
