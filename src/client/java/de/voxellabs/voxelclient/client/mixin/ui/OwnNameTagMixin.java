package de.voxellabs.voxelclient.client.mixin.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class OwnNameTagMixin {

    @Inject(
            method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void showOwnLabel(LivingEntity entity,
                              double squaredDistanceToCamera,
                              CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (entity != client.player) return;
        if (client.options.getPerspective().isFirstPerson()) return;

        cir.setReturnValue(true);
    }

}
