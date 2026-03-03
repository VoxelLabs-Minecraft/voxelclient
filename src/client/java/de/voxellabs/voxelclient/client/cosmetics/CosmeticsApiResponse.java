package de.voxellabs.voxelclient.client.cosmetics;

/**
 * API-Antwort von https://api.voxellabs.de/cosmetics/{uuid}
 *
 * Erwartetes JSON:
 * {
 *   "uuid": "...",
 *   "cosmetics": {
 *     "cape":  { "enabled": true,  "url": "https://cdn.voxellabs.de/capes/default.png" },
 *     "halo":  { "enabled": true,  "url": "https://cdn.voxellabs.de/halos/golden.png"  },
 *     "wings": { "enabled": false, "url": null },
 *     "trail": { "enabled": true,  "url": null, "id": "stars" }
 *   }
 * }
 */
public class CosmeticsApiResponse {

    public String uuid;
    public CosmeticsData cosmetics;

    public static class CosmeticsData {
        public CosmeticItem cape;
        public CosmeticItem halo;
        public CosmeticItem wings;
        public CosmeticItem trail;
    }

    public static class CosmeticItem {
        public boolean enabled;
        public String url;   // CDN-URL zur Textur
        public String id;    // Nur für Trail (Partikel-Typ)
    }

    public boolean hasCape()  { return cosmetics != null && cosmetics.cape  != null && cosmetics.cape.enabled  && cosmetics.cape.url  != null; }
    public boolean hasHalo()  { return cosmetics != null && cosmetics.halo  != null && cosmetics.halo.enabled  && cosmetics.halo.url  != null; }
    public boolean hasWings() { return cosmetics != null && cosmetics.wings != null && cosmetics.wings.enabled && cosmetics.wings.url != null; }
    public boolean hasTrail() { return cosmetics != null && cosmetics.trail != null && cosmetics.trail.enabled; }
}
