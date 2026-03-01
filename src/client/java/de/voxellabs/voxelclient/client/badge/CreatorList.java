package de.voxellabs.voxelclient.client.badge;

import java.util.Set;
import java.util.UUID;

/**
 * Liste aller VoxelClient-Creatoren.
 * Spieler in dieser Liste bekommen ein rotes Badge vor ihrem Namen.
 *
 * UUID eines Spielers herausfinden:
 *   https://namemc.com/<spielername>
 *   oder: https://api.mojang.com/users/profiles/minecraft/<spielername>
 */
public final class CreatorList {

    /**
     * UUIDs der VoxelClient-Creatoren.
     * ↓ Hier deine eigene UUID + weitere Creatoren eintragen
     */
    private static final Set<UUID> CREATORS = Set.of(
            UUID.fromString("00000000-0000-0000-0000-000000000000"), // ← deine UUID
            UUID.fromString("00000000-0000-0000-0000-000000000001")  // ← weiterer Creator
    );

    private CreatorList() {}

    /**
     * Gibt zurück ob der Spieler ein Creator ist.
     *
     * @param uuid UUID des Spielers
     * @return true wenn Creator
     */
    public static boolean isCreator(UUID uuid) {
        if (uuid == null) return false;
        return CREATORS.contains(uuid);
    }
}