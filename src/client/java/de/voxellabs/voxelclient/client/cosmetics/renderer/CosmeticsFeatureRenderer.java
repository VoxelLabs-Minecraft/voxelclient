package de.voxellabs.voxelclient.client.cosmetics.renderer;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiResponse;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsCatalog;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsCatalogClient;
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
 *
 * Logik:
 *   Eigener Spieler → aktives Item kommt aus VoxelClientConfig (lokale Auswahl)
 *   Andere Spieler  → aktives Item kommt aus CosmeticsApiResponse.active (Server)
 */
public class CosmeticsFeatureRenderer
        extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public CosmeticsFeatureRenderer(
            FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    /** Einmalig beim Start aufrufen — registriert den Trail-Ticker. */
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

        // Katalog muss geladen sein – ohne URL können wir nichts rendern
        CosmeticsCatalog catalog = CosmeticsCatalogClient.get();
        if (catalog == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isOwnPlayer = mc.player != null && mc.player.getUuid().equals(uuid);

        // ── Halo ──────────────────────────────────────────────────────────────
        String haloUrl = resolveUrl(uuid, "halo", isOwnPlayer, catalog);
        if (haloUrl != null) {
            HaloRenderer.render(matrices, vertexConsumers, light, state, 0f, haloUrl);
        }

        // ── Wings ─────────────────────────────────────────────────────────────
        String wingsUrl = resolveUrl(uuid, "wings", isOwnPlayer, catalog);
        if (wingsUrl != null) {
            WingsRenderer.render(matrices, vertexConsumers, light, state, 0f, wingsUrl);
        }

        // Trail läuft über TrailRenderer (Tick-Event), nicht hier
    }

    /**
     * Bestimmt die Textur-URL für ein Cosmetic-Item.
     *
     * 1. Aktive Item-ID ermitteln:
     *    - Eigener Spieler → VoxelClientConfig (lokale Auswahl im Menü)
     *    - Anderer Spieler → CosmeticsApiResponse.active (Server-State)
     * 2. Item anhand der ID im Katalog nachschlagen → URL zurückgeben.
     *
     * Gibt null zurück wenn kein Item aktiv ist oder kein URL vorhanden.
     */
    private static String resolveUrl(UUID uuid, String typeName,
                                     boolean isOwnPlayer,
                                     CosmeticsCatalog catalog) {
        int itemId;

        if (isOwnPlayer) {
            // Eigener Spieler: lokale Config ist maßgeblich
            itemId = VoxelClientConfig.get().getActiveItemId(typeName);
        } else {
            // Anderer Spieler: Server-Antwort
            CosmeticsApiResponse data = CosmeticsApiClient.getCosmetics(uuid);
            if (data == null) return null;
            itemId = data.getActiveItemId(typeName);
        }

        if (itemId == 0) return null; // kein Item aktiv

        CosmeticsCatalog.CatalogItem item = catalog.findItem(itemId);
        if (item == null || item.url == null || item.url.isBlank()) return null;

        return item.url;
    }
}