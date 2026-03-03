package de.voxellabs.voxelclient.client.cosmetics.renderer;

import de.voxellabs.voxelclient.client.cosmetics.utility.WebTextureLoader;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Halo Renderer — VoxelClient Cosmetics
 * Rendert einen rotierenden Halo-Ring über dem Spielerkopf.
 * Textur wird dynamisch von der CDN-URL geladen.
 */
public class HaloRenderer {

    private static final float HALO_SIZE = 0.55f;
    private static final float HALO_Y    = 0.35f;

    public static void render(MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers,
                               int light,
                               PlayerEntityRenderState state,
                               float tickDelta,
                               String textureUrl) {

        Identifier texture = WebTextureLoader.getOrLoad(
                textureUrl, "halo_" + Math.abs(textureUrl.hashCode()));
        if (texture == null) return;

        matrices.push();

        // Über den Kopf positionieren
        matrices.translate(0.0f, state.standingEyeHeight + HALO_Y, 0.0f);

        // Langsam rotieren
        float rotation = (System.currentTimeMillis() % 8000L) / 8000.0f * 360.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

        // Leichte Neigung für 3D-Effekt
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0f));

        // Schwebe-Animation
        float bob = MathHelper.sin((System.currentTimeMillis() % 2000L) / 2000.0f * MathHelper.TAU) * 0.025f;
        matrices.translate(0.0f, bob, 0.0f);

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float s = HALO_SIZE;

        vc.vertex(matrix, -s, 0, -s).color(255, 255, 255, 200)
                .texture(0f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 1, 0);
        vc.vertex(matrix, -s, 0,  s).color(255, 255, 255, 200)
                .texture(0f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 1, 0);
        vc.vertex(matrix,  s, 0,  s).color(255, 255, 255, 200)
                .texture(1f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 1, 0);
        vc.vertex(matrix,  s, 0, -s).color(255, 255, 255, 200)
                .texture(1f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 1, 0);

        matrices.pop();
    }
}
