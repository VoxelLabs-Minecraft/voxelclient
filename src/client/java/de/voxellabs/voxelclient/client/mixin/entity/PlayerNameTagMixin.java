package de.voxellabs.voxelclient.client.mixin.entity;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.utils.VoxelClientNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

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

        String prefix = resolveBadgePrefix(uuid);
        if (prefix == null) return;
        if (!state.name.startsWith(prefix))
            state.name = prefix + state.name;
    }

    private static String resolveBadgePrefix(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isOwnPlayer = mc.player != null && mc.player.getUuid().equals(uuid);

        if (isOwnPlayer) {
            int activeId = VoxelClientConfig.get().activeBadgeId;
            if (activeId == 0) return null;
            BadgeApiClient.CachedBadge badge = BadgeApiClient.getBadgeById(activeId);
            if (badge == null) return null;
            return BadgeApiClient.formatColor(badge.color)
                    + (badge.icon != null ? badge.icon : "✦") + " §r";
        }

        BadgeApiClient.CachedBadge badge = BadgeApiClient.getBadge(uuid);
        if (badge == null) return null;
        return BadgeApiClient.formatColor(badge.color)
                + (badge.icon != null ? badge.icon : "✦") + " §r";
    }
}