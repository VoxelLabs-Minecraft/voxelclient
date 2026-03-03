package de.voxellabs.voxelclient.client.mixin.cosmetics;

import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import de.voxellabs.voxelclient.client.cosmetics.utility.CosmeticsStateMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Speichert die UUID des Spielers im RenderState und
 * startet automatisch einen Cosmetics-Prefetch.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class CosmeticsStateMixin {

    @Inject(
        method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void captureUuid(AbstractClientPlayerEntity entity,
                              PlayerEntityRenderState state,
                              float tickDelta,
                              CallbackInfo ci) {
        UUID uuid = entity.getUuid();
        CosmeticsStateMap.put(state, uuid);
        CosmeticsApiClient.prefetch(uuid);
    }
}
