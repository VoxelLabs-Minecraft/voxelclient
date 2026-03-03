package de.voxellabs.voxelclient.client.mixin.cosmetics;

import de.voxellabs.voxelclient.client.cosmetics.renderer.CosmeticsFeatureRenderer;
import de.voxellabs.voxelclient.client.cosmetics.utility.CosmeticsStateMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class CosmeticsRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {

    public CosmeticsRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addCosmeticFeatures(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.addFeature(new CosmeticsFeatureRenderer(this));
        CosmeticsFeatureRenderer.register();
    }

    // Nur UUID in die StateMap schreiben – KEIN API-Call hier!
    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void captureUuid(AbstractClientPlayerEntity player,
                             PlayerEntityRenderState state,
                             float tickDelta, CallbackInfo ci) {
        CosmeticsStateMap.put(state, player.getUuid());
    }
}