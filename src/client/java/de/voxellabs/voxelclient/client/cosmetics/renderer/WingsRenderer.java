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
 * Wings Renderer — VoxelClient Cosmetics
 * Rendert Flügel auf dem Rücken des Spielers.
 * Die Wings-Textur wird als einzelnes Sprite von der CDN-URL geladen.
 * Linke Seite = linke Hälfte der Textur, rechte Seite = rechte Hälfte (gespiegelt).
 */
public class WingsRenderer {

    private static final float WING_W = 0.8f;
    private static final float WING_H = 0.9f;
    private static final float WING_Y = 1.2f;

    public static void render(MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers,
                               int light,
                               PlayerEntityRenderState state,
                               float tickDelta,
                               String textureUrl) {

        Identifier texture = WebTextureLoader.getOrLoad(
                textureUrl, "wings_" + Math.abs(textureUrl.hashCode()));
        if (texture == null) return;

        // Flügel-Schlag Animation
        float time = (System.currentTimeMillis() % 1500L) / 1500.0f;
        boolean isMoving = state.limbFrequency > 0.01f;
        float beatSpeed = isMoving ? 2.5f : 0.8f;
        float beatAmp   = isMoving ? 18.0f : 8.0f;
        float flapAngle = MathHelper.sin(time * MathHelper.TAU * beatSpeed) * beatAmp;

        // Linker Flügel
        matrices.push();
        matrices.translate(0.0f, WING_Y, 0.1f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(flapAngle));
        renderWingQuad(matrices, vertexConsumers, light, texture, false);
        matrices.pop();

        // Rechter Flügel (gespiegelt)
        matrices.push();
        matrices.translate(0.0f, WING_Y, 0.1f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-flapAngle));
        renderWingQuad(matrices, vertexConsumers, light, texture, true);
        matrices.pop();
    }

    private static void renderWingQuad(MatrixStack matrices,
                                        VertexConsumerProvider vertexConsumers,
                                        int light,
                                        Identifier texture,
                                        boolean mirrored) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float w = WING_W * (mirrored ? -1 : 1);
        float h = WING_H;

        // UV: linke Hälfte für linken Flügel, rechte Hälfte für rechten
        float u0 = mirrored ? 0.5f : 0.0f;
        float u1 = mirrored ? 1.0f : 0.5f;

        vc.vertex(matrix, 0, 0, 0).color(255, 255, 255, 230)
                .texture(mirrored ? u1 : u0, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        vc.vertex(matrix, 0, h, 0).color(255, 255, 255, 230)
                .texture(mirrored ? u1 : u0, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        vc.vertex(matrix, w, h, 0).color(255, 255, 255, 230)
                .texture(mirrored ? u0 : u1, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        vc.vertex(matrix, w, 0, 0).color(255, 255, 255, 230)
                .texture(mirrored ? u0 : u1, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
    }
}
