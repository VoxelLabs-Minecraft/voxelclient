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
            UUID.fromString("1737d252-1644-46e9-bfcb-8e9c0b56313a"), // ← deine UUID
            UUID.fromString("605878d2-1105-4bc8-891c-3c278de71892"),
            UUID.fromString("f4d7e09c-7e16-498a-b73f-4dac4cc2d6b9")// ← weiterer Creator
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
