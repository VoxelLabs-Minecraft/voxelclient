package de.voxellabs.voxelclient.client.mixin;

import de.voxellabs.voxelclient.client.badge.BadgeRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.text.Text;
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

        // name ist null wenn der Nametag nicht angezeigt werden soll (z.B. unsichtbar)
        if (state.name == null) return;

        UUID uuid = entity.getUuid();
        String badge = BadgeRenderer.getBadgeString(uuid);

        // Badge vor den bestehenden Namen stellen
        state.name = badge + state.name;
    }
}