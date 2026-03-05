package de.voxellabs.voxelclient.client.badge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.voxellabs.voxelclient.client.utils.VoxelClientNetwork;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Lädt Badge-Daten via Batch-Endpoint.
 *
 * Strategie:
 *  - UUIDs werden 1,5s gesammelt, dann ein POST /api/badges/batch
 *  - Eigener Spieler (fetchWithCallback): sofortiger Einzelrequest
 *  - Cache-TTL: 10 Minuten
 */
public final class BadgeApiClient {

    public static final String API_BASE_URL = "https://api.voxellabs.de";
    private static final String BATCH_URL   = API_BASE_URL + "/api/badges/batch";
    private static final String SINGLE_URL  = API_BASE_URL + "/api/players/";

    private static final long     CACHE_TTL_MS  = 10 * 60 * 1000L;
    private static final Duration TIMEOUT       = Duration.ofSeconds(6);
    private static final long     BATCH_WINDOW  = 1500L;
    private static final int      BATCH_MAX     = 100;

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private static final Gson GSON = new Gson();

    // ── Cache ─────────────────────────────────────────────────────────────────
    public static class CachedBadge {
        public final String name, display, color, icon;
        final long fetchedAt;
        CachedBadge(String name, String display, String color, String icon, long fetchedAt) {
            this.name = name; this.display = display;
            this.color = color; this.icon = icon; this.fetchedAt = fetchedAt;
        }
        public boolean isExpired() {
            return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS;
        }
    }

    private static final CachedBadge NO_BADGE =
            new CachedBadge(null, null, null, null, Long.MAX_VALUE / 2);

    private static final Map<UUID, CachedBadge>            CACHE     = new ConcurrentHashMap<>();
    private static final Set<UUID>                         LOADING   = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, List<Consumer<CachedBadge>>> CALLBACKS = new ConcurrentHashMap<>();

    // ── Batch-Sammler ─────────────────────────────────────────────────────────
    private static final Set<UUID>            BATCH_PENDING = ConcurrentHashMap.newKeySet();
    private static volatile ScheduledFuture<?> batchTimer   = null;
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "VoxelClient-BadgeBatch");
                t.setDaemon(true);
                return t;
            });

    // ── Globaler Badge-Katalog (id → Badge) ──────────────────────────────────
    // Wird von CosmeticsCatalogClient/ClientModScreen via loadAllBadges() befüllt.
    private static final java.util.concurrent.ConcurrentHashMap<Integer, CachedBadge>
            CATALOG = new java.util.concurrent.ConcurrentHashMap<>();

    /** Speichert alle bekannten Badges aus dem öffentlichen Katalog. */
    public static void putCatalogBadge(int id, String name, String display, String color, String icon) {
        CATALOG.put(id, new CachedBadge(name, display, color, icon, Long.MAX_VALUE / 2));
    }

    /** Gibt ein Badge anhand seiner ID zurück (aus dem Katalog, unabhängig vom Spieler). */
    public static CachedBadge getBadgeById(int id) {
        return CATALOG.get(id);
    }

    private BadgeApiClient() {}

    // ── Public API ────────────────────────────────────────────────────────────

    public static CachedBadge getBadge(UUID uuid) {
        CachedBadge c = CACHE.get(uuid);
        if (c != null && !c.isExpired()) return c == NO_BADGE ? null : c;
        prefetch(uuid);
        return (c == null || c == NO_BADGE) ? null : c;
    }

    public static String getBadgeString(UUID uuid) {
        CachedBadge badge = getBadge(uuid);
        if (badge == null) return "§7✦ §r";
        return formatColor(badge.color) + (badge.icon != null ? badge.icon : "✦") + " §r";
    }

    /** Fügt UUID in Batch-Queue ein. Nur für bestätigte VoxelClient-Nutzer aufrufen. */
    public static void prefetch(UUID uuid) {
        CachedBadge c = CACHE.get(uuid);
        if (c != null && !c.isExpired()) return;
        if (!LOADING.add(uuid)) return;

        BATCH_PENDING.add(uuid);

        if (batchTimer == null || batchTimer.isDone()) {
            batchTimer = SCHEDULER.schedule(
                    BadgeApiClient::flushBatch, BATCH_WINDOW, TimeUnit.MILLISECONDS);
        }
        if (BATCH_PENDING.size() >= BATCH_MAX) {
            if (batchTimer != null) batchTimer.cancel(false);
            SCHEDULER.execute(BadgeApiClient::flushBatch);
        }
    }

    /** Sofortiger Einzelrequest für den eigenen Spieler. */
    public static void fetchWithCallback(UUID uuid, Consumer<CachedBadge> callback) {
        CachedBadge c = CACHE.get(uuid);
        if (c != null && !c.isExpired()) {
            callback.accept(c == NO_BADGE ? null : c);
            return;
        }
        CALLBACKS.computeIfAbsent(uuid, k -> new ArrayList<>()).add(callback);
        if (LOADING.add(uuid)) {
            Thread.ofVirtual().start(() -> fetchSingle(uuid));
        }
    }

    public static void clearCache() {
        CACHE.clear();
        LOADING.clear();
        BATCH_PENDING.clear();
        CALLBACKS.clear();
    }

    // ── Batch ─────────────────────────────────────────────────────────────────

    private static synchronized void flushBatch() {
        if (BATCH_PENDING.isEmpty()) return;
        List<UUID> batch = new ArrayList<>(BATCH_PENDING);
        BATCH_PENDING.clear();

        System.out.println("[VoxelClient] Badge Batch-Request: " + batch.size() + " UUIDs");

        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        batch.forEach(u -> arr.add(u.toString()));
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.add("uuids", arr);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BATCH_URL))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent", "VoxelClient/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) return resp.body();
                System.err.println("[VoxelClient] Badge-Batch HTTP " + resp.statusCode());
                return null;
            } catch (Exception e) {
                System.err.println("[VoxelClient] Badge-Batch Fehler: " + e.getMessage());
                return null;
            }
        }).handle((responseBody, ex) -> {
            batch.forEach(LOADING::remove);
            if (responseBody == null) {
                batch.forEach(uuid -> storeAndFire(uuid, null));
                return null;
            }
            try {
                JsonObject map = GSON.fromJson(responseBody, JsonObject.class);
                for (UUID uuid : batch) {
                    CachedBadge badge = null;
                    if (map.has(uuid.toString()) && !map.get(uuid.toString()).isJsonNull()) {
                        badge = parseBadge(map.getAsJsonObject(uuid.toString()));
                    }
                    storeAndFire(uuid, badge);
                }
            } catch (Exception e) {
                System.err.println("[VoxelClient] Badge-Batch Parse-Fehler: " + e.getMessage());
                batch.forEach(uuid -> storeAndFire(uuid, null));
            }
            return null;
        });
    }

    // ── Einzelrequest ─────────────────────────────────────────────────────────

    private static void fetchSingle(UUID uuid) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(SINGLE_URL + uuid))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "VoxelClient/1.0")
                    .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            CachedBadge badge = null;
            if (resp.statusCode() == 200) {
                JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
                badge = parseBadge(json);
            }
            LOADING.remove(uuid);
            storeAndFire(uuid, badge);
        } catch (Exception e) {
            System.err.println("[VoxelClient] Badge-Single Fehler: " + e.getMessage());
            LOADING.remove(uuid);
            storeAndFire(uuid, null);
        }
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private static CachedBadge parseBadge(JsonObject json) {
        if (json == null) return null;
        String name = str(json, "badge_name");
        if (name == null) return null; // kein Badge vergeben
        return new CachedBadge(
                name, str(json, "display"), str(json, "color"),
                str(json, "icon"), System.currentTimeMillis());
    }

    private static void storeAndFire(UUID uuid, CachedBadge badge) {
        CACHE.put(uuid, badge != null ? badge : NO_BADGE);
        if (badge != null) VoxelClientNetwork.addVoxelUser(uuid);
        List<Consumer<CachedBadge>> cbs = CALLBACKS.remove(uuid);
        if (cbs != null) cbs.forEach(cb -> cb.accept(badge));
    }

    private static String str(JsonObject json, String key) {
        return (json.has(key) && !json.get(key).isJsonNull())
                ? json.get(key).getAsString() : null;
    }

    public static String formatColor(String hex) {
        if (hex == null) return "§7";
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            if (r > 180 && g < 80  && b < 80)  return "§4";
            if (r > 220 && g < 120 && b < 80)  return "§c";
            if (r > 200 && g > 120 && b < 60)  return "§6";
            if (r > 200 && g > 200 && b < 80)  return "§e";
            if (r < 80  && g > 180 && b < 80)  return "§2";
            if (r < 120 && g > 200 && b < 120) return "§a";
            if (r < 80  && g < 80  && b > 180) return "§1";
            if (r < 120 && g < 120 && b > 220) return "§9";
            if (r > 100 && g < 80  && b > 180) return "§5";
            if (r > 180 && g < 120 && b > 200) return "§d";
            if (r < 80  && g > 180 && b > 180) return "§3";
            if (r > 160 && g > 200 && b > 200) return "§b";
        } catch (Exception ignored) {}
        return "§7";
    }
}