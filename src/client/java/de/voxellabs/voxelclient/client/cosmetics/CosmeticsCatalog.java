package de.voxellabs.voxelclient.client.cosmetics;

import java.util.List;

/**
 * Antwort von GET /api/cosmetics/catalog
 *
 * Enthält ALLE verfügbaren Cosmetic-Typen und deren Items.
 * Wird einmalig beim Start gecacht und dann für alle Spieler genutzt.
 *
 * JSON-Struktur:
 * {
 *   "types": [
 *     {
 *       "id": 1, "name": "cape", "display": "Cape", "description": "Umhang",
 *       "items": [
 *         { "id": 1, "name": "default_cape", "display": "Default Cape",
 *           "url": "https://cdn.voxellabs.de/capes/default.png",
 *           "trail_id": null, "description": "Der klassische Cape" }
 *       ]
 *     }
 *   ]
 * }
 */
public class CosmeticsCatalog {

    public List<CatalogType> types;

    public static class CatalogType {
        public int    id;
        public String name;
        public String display;
        public String description;
        public List<CatalogItem> items;
    }

    public static class CatalogItem {
        public int    id;
        public String name;
        public String display;
        public String url;          // CDN-URL zur Vorschau-Textur (null bei Trails)
        public String trail_id;     // Partikel-ID, nur bei Trail-Items gesetzt
        public String description;
    }

    /** Gibt alle Items eines bestimmten Typs zurück (per type name: "cape", "halo", ...) */
    public List<CatalogItem> getItemsForType(String typeName) {
        if (types == null) return List.of();
        for (CatalogType type : types) {
            if (typeName.equals(type.name)) {
                return type.items != null ? type.items : List.of();
            }
        }
        return List.of();
    }

    /** Findet ein Item anhand seiner ID */
    public CatalogItem findItem(int itemId) {
        if (types == null) return null;
        for (CatalogType type : types) {
            if (type.items == null) continue;
            for (CatalogItem item : type.items) {
                if (item.id == itemId) return item;
            }
        }
        return null;
    }

    /** Prüft ob der Katalog tatsächlich Items enthält */
    public boolean isEmpty() {
        if (types == null || types.isEmpty()) return true;
        return types.stream().allMatch(t -> t.items == null || t.items.isEmpty());
    }
}