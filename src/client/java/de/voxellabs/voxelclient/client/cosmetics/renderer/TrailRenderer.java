package de.voxellabs.voxelclient.client.cosmetics.renderer;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiResponse;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsCatalog;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsCatalogClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Trail Renderer — VoxelClient Cosmetics
 * Spawnt Partikel-Trails hinter Spielern her (Tick-basiert).
 *
 * Trail-IDs (in cosmetic_items.trail_id):
 *   flame   → Flammen
 *   hearts  → Herzen
 *   stars   → End Rod Sterne
 *   magic   → Enchantment-Partikel
 *   rainbow → Farbige Dust-Partikel
 */
public class TrailRenderer {

    private static final int SPAWN_INTERVAL = 2;
    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;
            tickCounter++;
            if (tickCounter < SPAWN_INTERVAL) return;
            tickCounter = 0;

            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                spawnTrailForPlayer(client, player);
            }
        });
    }

    private static void spawnTrailForPlayer(MinecraftClient client,
                                            AbstractClientPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean isOwnPlayer = client.player != null && client.player.getUuid().equals(uuid);

        // Trail-Item-ID ermitteln
        int trailItemId = resolveActiveTrailItemId(uuid, isOwnPlayer);
        if (trailItemId == 0) return;

        // Trail-ID (Partikeltyp) aus dem Katalog holen
        String trailId = resolveTrailId(trailItemId);
        if (trailId == null || trailId.isBlank()) return;

        // Nur spawnen wenn sich der Spieler bewegt
        Vec3d vel = player.getVelocity();
        boolean moving = vel.horizontalLength() > 0.05 || Math.abs(vel.y) > 0.1;
        if (!moving && player.isOnGround()) return;

        double x = player.getX();
        double y = player.getY() + 0.1;
        double z = player.getZ();

        spawnParticles(client, trailId, x, y, z);
    }

    /**
     * Gibt die aktive Trail-Item-ID zurück.
     *
     * Eigener Spieler: lokale Config ist maßgeblich (Spieler hat im Screen
     *                  ein Item ausgewählt → das wird gerendert).
     * Andere Spieler:  Server-Antwort (CosmeticsApiResponse.active) ist
     *                  maßgeblich.
     *
     * Gibt 0 zurück wenn kein Trail aktiv ist.
     */
    private static int resolveActiveTrailItemId(UUID uuid, boolean isOwnPlayer) {
        if (isOwnPlayer) {
            return VoxelClientConfig.get().getActiveItemId("trail");
        }

        CosmeticsApiResponse data = CosmeticsApiClient.getCosmetics(uuid);
        if (data == null) return 0;
        return data.getActiveItemId("trail");
    }

    /**
     * Schlägt im Katalog nach welche trail_id ein Item hat.
     * Gibt null zurück wenn Katalog noch nicht geladen oder Item nicht gefunden.
     */
    private static String resolveTrailId(int itemId) {
        CosmeticsCatalog catalog = CosmeticsCatalogClient.get();
        if (catalog == null) return null;
        CosmeticsCatalog.CatalogItem item = catalog.findItem(itemId);
        if (item == null) return null;
        return item.trail_id;
    }

    // ── Partikel-Spawning ─────────────────────────────────────────────────────

    private static void spawnParticles(MinecraftClient client, String trailId,
                                       double x, double y, double z) {
        if (client.world == null) return;

        switch (trailId) {
            case "flame" -> {
                for (int i = 0; i < 3; i++) {
                    client.world.addParticle(ParticleTypes.FLAME,
                            x + rnd(), y, z + rnd(), 0, 0.02, 0);
                }
            }
            case "hearts" -> {
                if (Math.random() < 0.4) {
                    client.world.addParticle(ParticleTypes.HEART,
                            x + rnd() * 0.5, y + 0.5, z + rnd() * 0.5, 0, 0.05, 0);
                }
            }
            case "stars" -> {
                for (int i = 0; i < 2; i++) {
                    client.world.addParticle(ParticleTypes.END_ROD,
                            x + rnd(), y + Math.random() * 0.5, z + rnd(),
                            rnd() * 0.05, 0.03, rnd() * 0.05);
                }
            }
            case "rainbow" -> {
                float hue = ((float) System.currentTimeMillis() / 200 % 100) / 100.0f;
                int rgb  = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
                int argb = 0xFF000000 | (rgb & 0x00FFFFFF);
                for (int i = 0; i < 3; i++) {
                    client.world.addParticle(
                            new DustParticleEffect(argb, 1.0f),
                            x + rnd(), y + Math.random() * 0.3, z + rnd(), 0, 0, 0);
                }
            }
            case "magic" -> {
                for (int i = 0; i < 4; i++) {
                    client.world.addParticle(ParticleTypes.ENCHANT,
                            x + rnd(), y + Math.random(), z + rnd(),
                            rnd() * 0.1, 0.05, rnd() * 0.1);
                }
            }
            // Unbekannte trail_id → kein Partikel
        }
    }

    private static double rnd() {
        return (Math.random() - 0.5) * 0.5;
    }
}