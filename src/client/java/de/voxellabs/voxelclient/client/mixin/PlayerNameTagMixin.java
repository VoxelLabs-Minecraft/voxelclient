package de.voxellabs.voxelclient.client.mixin;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.utils.VoxelClientNetwork;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Fügt das VoxelClient-Badge in den Nametag über dem Spielerkopf ein.
 *
 * Ziel-Methode: EntityRenderer#getDisplayName(EntityRenderState) → Text
 * In 1.21.4 wird der Anzeigename über den RenderState ermittelt.
 */
@Mixin(value = PlayerEntityRenderer.class)
public abstract class PlayerNameTagMixin {

    @Inject(
            method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void injectBadge(AbstractClientPlayerEntity entity,
                             PlayerEntityRenderState state,
                             float tickDelta,
                             CallbackInfo ci) {

        if (state.name == null) return;

        UUID uuid = entity.getUuid();

        if (!VoxelClientNetwork.isVoxelUser(uuid)) return;

        BadgeApiClient.CachedBadge badge = BadgeApiClient.getBadge(uuid);
        if (badge == null) return;

        String prefix =  BadgeApiClient.getBadgeString(uuid);
        if (!state.name.startsWith(prefix))
            state.name = prefix + state.name;
    }
}