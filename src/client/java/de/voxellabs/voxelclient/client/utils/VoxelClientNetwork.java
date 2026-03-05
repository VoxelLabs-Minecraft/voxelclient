package de.voxellabs.voxelclient.client.utils;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoxelClientNetwork {

    // ── Set aller Spieler die VoxelClient nutzen ──────────────────────────────
    private static final Set<UUID> VOXEL_USERS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private VoxelClientNetwork() {}

    /**
     * Gibt zurück, ob ein Spieler VoxelClient nutzt.
     */
    public static boolean isVoxelUser(UUID uuid) {
        return VOXEL_USERS.contains(uuid);
    }

    public static void addVoxelUser(UUID uuid) {
        VOXEL_USERS.add(uuid);
    }

    public static void removeVoxelUser(UUID uuid) {
        VOXEL_USERS.remove(uuid);
    }

    /**
     * Initialisiert das Netzwerk-System.
     * Aufruf in VoxelClientModClient.onInitializeClient()
     */
    public static void init() {

        // Payload registrieren – ID kommt aus HandshakePayload
        PayloadTypeRegistry.playC2S().register(HandshakePayload.ID, HandshakePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HandshakePayload.ID, HandshakePayload.CODEC);

        // Eingehende Pakete empfangen (anderer Spieler hat VoxelClient)
        ClientPlayNetworking.registerGlobalReceiver(
                HandshakePayload.ID,
                (payload, context) -> {
                    UUID sender = payload.uuid();
                    VOXEL_USERS.add(sender);

                    // Badge + Cosmetics laden sobald VoxelClient-Nutzer bestätigt
                    BadgeApiClient.prefetch(sender);
                    CosmeticsApiClient.prefetch(sender);
                }
        );

        // Beim Server-Join: eigenes Paket senden + eigene UUID registrieren
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player == null) return;
            UUID ownUuid = client.player.getUuid();
            VOXEL_USERS.add(ownUuid);
            ClientPlayNetworking.send(new HandshakePayload(ownUuid));
            BadgeApiClient.prefetch(ownUuid); // sofort laden
        });

        // Beim Disconnect: Liste leeren
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            VOXEL_USERS.clear();
            // Badge-Cache muss nicht geleert werden – TTL von 5min ist ok
        });
    }
}