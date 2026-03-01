package de.voxellabs.voxelclient.client.utils;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoxelClientNetwork {

    // ── Packet ID ─────────────────────────────────────────────────────────────
    public static final Identifier CHANNEL =
            Identifier.of("voxelclient", "handshake");

    // ── Set aller Spieler die VoxelClient nutzen ──────────────────────────────
    private static final Set<UUID> VOXEL_USERS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private VoxelClientNetwork() {}

    /**
     * Gibt zurück, ob ein Spieler VoxelClient nutzt.
     * Nur für diese Spieler wird die Badge-API abgefragt.
     */
    public static boolean isVoxelUser(UUID uuid) {
        return VOXEL_USERS.contains(uuid);
    }

    /**
     * Initialisiert das Netzwerk-System.
     * Aufruf in VoxelClientModClient.onInitializeClient()
     */
    public static void init() {

        // Payload registrieren
        PayloadTypeRegistry.playC2S().register(
                HandshakePayload.ID,
                HandshakePayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                HandshakePayload.ID,
                HandshakePayload.CODEC
        );

        // Eingehende Pakete empfangen (anderer Spieler hat VoxelClient)
        ClientPlayNetworking.registerGlobalReceiver(
                HandshakePayload.ID,
                (payload, context) -> {
                    UUID sender = payload.uuid();
                    VOXEL_USERS.add(sender);

                    // Badge asynchron vorladen
                    BadgeApiClient.getBadge(sender);
                }
        );

        // Beim Server-Join: eigenes Paket senden + eigene UUID registrieren
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player == null) return;

            UUID ownUuid = client.player.getUuid();
            VOXEL_USERS.add(ownUuid);

            // Allen anderen VoxelClient-Nutzern mitteilen, dass wir da sind
            ClientPlayNetworking.send(new HandshakePayload(ownUuid));

            // Eigenes Badge vorladen
            BadgeApiClient.getBadge(ownUuid);
        });

        // Beim Disconnect: Liste leeren
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                VOXEL_USERS.clear()
        );
    }
}
