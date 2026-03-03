package de.voxellabs.voxelclient.client.cosmetics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lädt Cosmetics (Cape, Halo, Wings, Trail) vom VoxelClient-Backend.
 *
 * API: GET /api/cosmetics/:uuid
 * Antwort:
 * {
 *   "uuid": "...",
 *   "cosmetics": {
 *     "cape":  { "enabled": true,  "url": "https://cdn.../cape.png", "id": null },
 *     "halo":  { "enabled": false, "url": null, "id": null },
 *     "wings": { "enabled": false, "url": null, "id": null },
 *     "trail": { "enabled": true,  "url": null, "id": "sparkle" }
 *   }
 * }
 */
public class CosmeticsManager {

    public static final String API_BASE = System.getProperty(
            "voxelclient.api.url", "https://api.voxellabs.de"
    );

    private static final Map<String, PlayerCosmetics> cache        = new ConcurrentHashMap<>();
    private static final Map<String, Boolean>         loading      = new ConcurrentHashMap<>();
    private static final Map<String, Identifier>      capeTextures = new ConcurrentHashMap<>();

    // ── Öffentliche API ───────────────────────────────────────────────────────

    public static void loadCosmetics(UUID uuid) { loadCosmetics(uuid.toString()); }

    public static void loadCosmetics(String uuid) {
        if (loading.getOrDefault(uuid, false)) return;
        loading.put(uuid, true);

        CompletableFuture.supplyAsync(() -> fetchJson(uuid))
                .thenAcceptAsync(json -> {
                    loading.put(uuid, false);
                    if (json == null) return;
                    PlayerCosmetics cosmetics = parse(uuid, json);
                    cache.put(uuid, cosmetics);
                    if (cosmetics.capeEnabled && cosmetics.capeUrl != null)
                        downloadCape(uuid, cosmetics.capeUrl);
                });
    }

    public static PlayerCosmetics getCosmetics(UUID uuid)   { return getCosmetics(uuid.toString()); }
    public static PlayerCosmetics getCosmetics(String uuid) { return cache.get(uuid); }

    public static Identifier getCapeTexture(UUID uuid)   { return getCapeTexture(uuid.toString()); }
    public static Identifier getCapeTexture(String uuid) {
        PlayerCosmetics c = cache.get(uuid);
        if (c == null || !c.capeEnabled) return null;
        return capeTextures.get(uuid);
    }

    public static void evict(UUID uuid)   { evict(uuid.toString()); }
    public static void evict(String uuid) {
        cache.remove(uuid);
        loading.remove(uuid);
        Identifier id = capeTextures.remove(uuid);
        if (id != null)
            MinecraftClient.getInstance().execute(() ->
                    MinecraftClient.getInstance().getTextureManager().destroyTexture(id));
    }

    public static void clearCache() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> capeTextures.values().forEach(mc.getTextureManager()::destroyTexture));
        cache.clear(); loading.clear(); capeTextures.clear();
    }

    public static boolean isLoading(String uuid) { return loading.getOrDefault(uuid, false); }
    public static boolean isCached(String uuid)  { return cache.containsKey(uuid); }

    // ── Intern ────────────────────────────────────────────────────────────────

    private static JsonObject fetchJson(String uuid) {
        try {
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create(API_BASE + "/api/cosmetics/" + uuid))
                    .GET().header("Accept", "application/json").build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                System.err.println("[VoxelClient] Cosmetics API " + resp.statusCode() + " für " + uuid);
                return null;
            }
            return JsonParser.parseString(resp.body()).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("[VoxelClient] Cosmetics Fehler: " + e.getMessage());
            return null;
        }
    }

    private static PlayerCosmetics parse(String uuid, JsonObject root) {
        PlayerCosmetics r = new PlayerCosmetics(uuid);
        if (!root.has("cosmetics")) return r;
        JsonObject c = root.getAsJsonObject("cosmetics");
        r.capeEnabled  = bool(c, "cape",  "enabled"); r.capeUrl      = str(c, "cape",  "url");
        r.haloEnabled  = bool(c, "halo",  "enabled"); r.haloUrl      = str(c, "halo",  "url");
        r.wingsEnabled = bool(c, "wings", "enabled"); r.wingsUrl     = str(c, "wings", "url");
        r.trailEnabled = bool(c, "trail", "enabled"); r.trailId      = str(c, "trail", "id");
        return r;
    }

    private static boolean bool(JsonObject c, String type, String field) {
        if (!c.has(type)) return false;
        JsonObject o = c.getAsJsonObject(type);
        return o.has(field) && !o.get(field).isJsonNull() && o.get(field).getAsBoolean();
    }

    private static String str(JsonObject c, String type, String field) {
        if (!c.has(type)) return null;
        JsonObject o = c.getAsJsonObject(type);
        if (!o.has(field) || o.get(field).isJsonNull()) return null;
        String v = o.get(field).getAsString();
        return v.isBlank() ? null : v;
    }

    private static void downloadCape(String uuid, String url) {
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient http = HttpClient.newHttpClient();
                HttpResponse<InputStream> resp = http.send(
                        HttpRequest.newBuilder(URI.create(url)).GET().build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) return null;
                return NativeImage.read(resp.body());
            } catch (Exception e) { return null; }
        }).thenAcceptAsync(image -> {
            if (image == null) return;
            Identifier id = Identifier.of("voxelclient", "cape_" + uuid.replace("-", ""));
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
                capeTextures.put(uuid, id);
            });
        });
    }

    // ── Datenklasse ───────────────────────────────────────────────────────────

    public static class PlayerCosmetics {
        public final String uuid;
        public boolean capeEnabled = false,  haloEnabled = false,
                wingsEnabled = false, trailEnabled = false;
        public String capeUrl = null, haloUrl = null, wingsUrl = null, trailId = null;

        public PlayerCosmetics(String uuid) { this.uuid = uuid; }
        public boolean hasAny() { return capeEnabled || haloEnabled || wingsEnabled || trailEnabled; }
    }
}