package de.voxellabs.voxelclient.client.mixin.entity;

import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager;
import de.voxellabs.voxelclient.client.utils.RenderStateUuidCache;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void captureUuid(AbstractClientPlayerEntity player,
                             PlayerEntityRenderState state,
                             float tickDelta, CallbackInfo ci) {
        RenderStateUuidCache.put(state, player.getUuid());

        if (!CosmeticsManager.isCached(player.getUuidAsString())) {
            CosmeticsManager.loadCosmetics(player.getUuid());
        }
    }
}
