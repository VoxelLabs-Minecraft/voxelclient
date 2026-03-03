package de.voxellabs.voxelclient.client.mixin.entity;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.model.EntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor<S extends LivingEntityRenderState,
        M extends EntityModel<? super S>> {

    @Invoker("addFeature")
    boolean invokeAddFeature(FeatureRenderer<S, M> feature);

}
