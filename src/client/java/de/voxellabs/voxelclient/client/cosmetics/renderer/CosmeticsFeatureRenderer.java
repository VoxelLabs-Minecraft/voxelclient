package de.voxellabs.voxelclient.client.cosmetics.renderer;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiResponse;
import de.voxellabs.voxelclient.client.cosmetics.utility.CosmeticsStateMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

import java.util.UUID;

/**
 * Haupt-FeatureRenderer für das VoxelClient Cosmetics-System.
 * Wird via CosmeticsRendererMixin in den PlayerEntityRenderer eingefügt.
 */
public class CosmeticsFeatureRenderer
        extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public CosmeticsFeatureRenderer(
            FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    /**
     * Einmalig beim Start aufrufen — registriert den Trail-Ticker.
     */
    public static void register() {
        TrailRenderer.register();
    }

    @Override
    public void render(MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light,
                       PlayerEntityRenderState state,
                       float limbAngle,
                       float limbDistance) {

        UUID uuid = CosmeticsStateMap.get(state);
        if (uuid == null) return;

        CosmeticsApiResponse cosmetics = CosmeticsApiClient.getCosmetics(uuid);
        if (cosmetics == null) return;

        VoxelClientConfig cfg =  VoxelClientConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isOwnPlayer = mc.player != null && mc.player.getUuid().equals(uuid);

        if (cosmetics.hasHalo() && (!isOwnPlayer || cfg.cosmeticHaloEnabled)) {
            HaloRenderer.render(matrices, vertexConsumers, light, state, 0f,
                    cosmetics.cosmetics.halo.url);
        }

        if (cosmetics.hasWings() && (!isOwnPlayer || cfg.cosmeticWingsEnabled)) {
            WingsRenderer.render(matrices, vertexConsumers, light, state, 0f,
                    cosmetics.cosmetics.wings.url);
        }

        // Trail läuft über TrailRenderer (Tick-Event), nicht hier
    }
}
