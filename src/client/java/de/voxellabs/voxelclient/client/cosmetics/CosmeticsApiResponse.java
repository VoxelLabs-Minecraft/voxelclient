package de.voxellabs.voxelclient.client.cosmetics;

import java.util.List;
import java.util.Map;

/**
 * Antwort von GET /api/cosmetics/{uuid}
 *
 * Enthält:
 *  - owned_item_ids: alle Item-IDs die dieser Spieler besitzt
 *  - active: welches Item pro Typ gerade aktiv ist
 *
 * JSON-Struktur:
 * {
 *   "uuid": "...",
 *   "owned_item_ids": [1, 3, 7],
 *   "active": {
 *     "cape":  { "item_id": 1,    "enabled": true  },
 *     "halo":  { "item_id": null, "enabled": false },
 *     "wings": { "item_id": null, "enabled": false },
 *     "trail": { "item_id": 7,    "enabled": true  }
 *   }
 * }
 */
public class CosmeticsApiResponse {

    public String        uuid;
    public List<Integer> owned_item_ids;   // Cosmetic-Items die der Spieler besitzt
    public List<Integer> owned_badge_ids;  // Badges die der Spieler besitzt
    public Map<String, ActiveState> active;

    public static class ActiveState {
        public Integer item_id;  // null = kein aktives Item
        public boolean enabled;
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    /** Prüft ob der Spieler ein bestimmtes Cosmetic-Item besitzt. */
    public boolean ownsItem(int itemId) {
        return owned_item_ids != null && owned_item_ids.contains(itemId);
    }

    /** Prüft ob der Spieler ein bestimmtes Badge besitzt. */
    public boolean ownsBadge(int badgeId) {
        return owned_badge_ids != null && owned_badge_ids.contains(badgeId);
    }

    /** Gibt den aktiven Item-State für einen Typ zurück (null wenn nicht vorhanden). */
    public ActiveState getActive(String typeName) {
        return active != null ? active.get(typeName) : null;
    }

    /** Gibt die aktuell aktive Item-ID für einen Typ zurück (0 = keine). */
    public int getActiveItemId(String typeName) {
        ActiveState state = getActive(typeName);
        if (state == null || state.item_id == null) return 0;
        return state.item_id;
    }

    /** Prüft ob ein bestimmtes Item gerade aktiv ist. */
    public boolean isItemActive(String typeName, int itemId) {
        return getActiveItemId(typeName) == itemId;
    }
}