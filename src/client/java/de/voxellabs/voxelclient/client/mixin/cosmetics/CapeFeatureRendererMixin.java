package de.voxellabs.voxelclient.client.mixin.cosmetics;

import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager;
import de.voxellabs.voxelclient.client.utils.RenderStateUuidCache;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

/**
 * Injects a custom cape texture override into the vanilla CapeFeatureRenderer.
 *
 * The injection happens at the point where the renderer normally checks for
 * the player's cape texture. If MyClient has a custom cape loaded, we redirect
 * to our dynamic texture instead.
 */
@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {

    /**
     * Cancels the default cape render and replaces it with the custom cape
     * from CosmeticsManager, if one is available.
     *
     * Strategy: We cancel the entire method only when our cape is ready,
     * then manually call the super-render logic pointing at our texture.
     * For simplicity, this implementation hooks BEFORE the method body
     * executes and re-routes texture look-up.
     */
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
                          int i, PlayerEntityRenderState playerEntityRenderState,
                          float f, float g, CallbackInfo ci) {

        UUID uuid = RenderStateUuidCache.get(playerEntityRenderState);
        if (uuid == null) return;

        Identifier capeTexture = CosmeticsManager.getCapeTexture(uuid);
        if (capeTexture == null) return;
        // TODO: Cape-Geometrie mit capeTexture rendern
    }
}
