package de.voxellabs.voxelclient.client.mixin.entity;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/**
 * Erweitert PlayerEntityRenderState um ein öffentliches UUID-Feld.
 * Wird von CosmeticsStateMixin befüllt und von CosmeticsFeatureRenderer gelesen.
 */
@Mixin(PlayerEntityRenderState.class)
public class PlayerRenderStateMixin {

    @Unique
    public UUID uuid = null;
}
