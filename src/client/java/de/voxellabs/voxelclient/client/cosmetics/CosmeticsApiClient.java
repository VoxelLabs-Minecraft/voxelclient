package de.voxellabs.voxelclient.client.cosmetics;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Lädt Cosmetics-Daten vom VoxelLabs-API-Server.
 *
 * Rate-Limit-Strategie:
 *   - Nur für bestätigte VoxelClient-Nutzer fetchen (via Handshake)
 *   - Batch-Requests: UUIDs werden 1,5s gesammelt, dann ein POST /api/cosmetics/batch
 *   - Eigener Spieler: sofortige Einzelanfrage (fetchWithCallback)
 *   - Cache-TTL: 10 Minuten
 */
public class CosmeticsApiClient {

    private static final String BASE_URL  = "https://api.voxellabs.de/api/cosmetics/";
    private static final String BATCH_URL = "https://api.voxellabs.de/api/cosmetics/batch";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    // Cache: 10 Minuten TTL
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;

    private static final Map<UUID, CosmeticsApiResponse>                 CACHE     = new ConcurrentHashMap<>();
    private static final Map<UUID, Long>                                  CACHED_AT = new ConcurrentHashMap<>();
    private static final Set<UUID>                                        LOADING   = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, List<Consumer<CosmeticsApiResponse>>>  CALLBACKS = new ConcurrentHashMap<>();

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private static final Gson GSON = new Gson();

    // ── Batch-Sammler ─────────────────────────────────────────────────────────
    // UUIDs werden gesammelt und alle BATCH_WINDOW_MS als ein einziger Request gesendet.
    private static final long BATCH_WINDOW_MS = 1500L;
    private static final int  BATCH_MAX_SIZE  = 50;   // max UUIDs pro Batch-Request

    private static final Set<UUID> BATCH_PENDING = ConcurrentHashMap.newKeySet();
    private static volatile ScheduledFuture<?> batchTimer = null;
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "VoxelClient-BatchScheduler");
                t.setDaemon(true);
                return t;
            });

    // ── Public API ────────────────────────────────────────────────────────────

    public static CosmeticsApiResponse getCosmetics(UUID uuid) {
        CosmeticsApiResponse cached = CACHE.get(uuid);
        if (cached != null && !isExpired(uuid)) return cached;
        prefetch(uuid);
        return cached; // ggf. veraltete Daten zurückgeben bis Refresh fertig
    }

    public static boolean isCached(UUID uuid) {
        return CACHE.containsKey(uuid);
    }

    /**
     * Fügt UUID in den Batch-Sammler ein.
     * Wird NICHT direkt für andere Spieler aufgerufen — nur nach Handshake-Bestätigung.
     */
    public static void prefetch(UUID uuid) {
        if (CACHE.containsKey(uuid) && !isExpired(uuid)) return;
        if (!LOADING.add(uuid)) return;

        BATCH_PENDING.add(uuid);

        // Batch-Timer starten (wird resettet wenn innerhalb des Fensters neue UUIDs kommen)
        if (batchTimer == null || batchTimer.isDone()) {
            batchTimer = SCHEDULER.schedule(
                    CosmeticsApiClient::flushBatch,
                    BATCH_WINDOW_MS, TimeUnit.MILLISECONDS
            );
        }

        // Sofort flushen wenn Batch-Limit erreicht
        if (BATCH_PENDING.size() >= BATCH_MAX_SIZE) {
            if (batchTimer != null) batchTimer.cancel(false);
            SCHEDULER.execute(CosmeticsApiClient::flushBatch);
        }
    }

    /**
     * Lädt Daten für den eigenen Spieler sofort (kein Batch, kein Delay).
     * Callback wird aufgerufen sobald Daten da sind — auch wenn null (Fehler/nicht registriert).
     */
    public static void fetchWithCallback(UUID uuid, Consumer<CosmeticsApiResponse> callback) {
        CosmeticsApiResponse cached = CACHE.get(uuid);
        if (cached != null && !isExpired(uuid)) {
            callback.accept(cached);
            return;
        }
        CALLBACKS.computeIfAbsent(uuid, k -> new ArrayList<>()).add(callback);
        if (LOADING.add(uuid)) {
            // Direkter Einzelrequest, kein Batch
            fetchSingle(uuid);
        }
    }

    public static void invalidate(UUID uuid) {
        CACHE.remove(uuid);
        CACHED_AT.remove(uuid);
        LOADING.remove(uuid);
        BATCH_PENDING.remove(uuid);
        CALLBACKS.remove(uuid);
    }

    public static void clearCache() {
        CACHE.clear();
        CACHED_AT.clear();
        LOADING.clear();
        BATCH_PENDING.clear();
        CALLBACKS.clear();
    }

    // ── Batch-Logik ───────────────────────────────────────────────────────────

    private static synchronized void flushBatch() {
        if (BATCH_PENDING.isEmpty()) return;

        Set<UUID> batch = new HashSet<>(BATCH_PENDING);
        BATCH_PENDING.clear();

        if (batch.isEmpty()) return;

        List<UUID> uuids = new ArrayList<>(batch);
        System.out.println("[VoxelClient] Cosmetics Batch-Request: " + uuids.size() + " UUIDs");

        // JSON-Body bauen
        JsonObject body = new JsonObject();
        JsonArray arr = new JsonArray();
        uuids.forEach(u -> arr.add(u.toString()));
        body.add("uuids", arr);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BATCH_URL))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent", "VoxelClient/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    return resp.body();
                }
                System.err.println("[VoxelClient] Batch-API " + resp.statusCode());
                return null;
            } catch (Exception e) {
                System.err.println("[VoxelClient] Batch-Fehler: " + e.getMessage());
                return null;
            }
        }).handle((body2, ex) -> {
            // Immer alle LOADING-Flags entfernen
            uuids.forEach(LOADING::remove);

            if (body2 == null) {
                // Fehler → Callbacks mit null feuern
                uuids.forEach(uuid -> fireCallbacks(uuid, null));
                return null;
            }
            try {
                // Response: { "uuid1": {...}, "uuid2": {...} }
                JsonObject responseMap = GSON.fromJson(body2, JsonObject.class);
                for (UUID uuid : uuids) {
                    CosmeticsApiResponse result = null;
                    if (responseMap.has(uuid.toString())) {
                        result = GSON.fromJson(
                                responseMap.get(uuid.toString()),
                                CosmeticsApiResponse.class
                        );
                    }
                    if (result != null) {
                        CACHE.put(uuid, result);
                        CACHED_AT.put(uuid, System.currentTimeMillis());
                    }
                    fireCallbacks(uuid, result);
                }
            } catch (Exception e) {
                System.err.println("[VoxelClient] Batch-Parse-Fehler: " + e.getMessage());
                uuids.forEach(uuid -> fireCallbacks(uuid, null));
            }
            return null;
        });
    }

    // ── Einzelrequest ─────────────────────────────────────────────────────────

    private static void fetchSingle(UUID uuid) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + uuid))
                .timeout(TIMEOUT)
                .header("User-Agent", "VoxelClient/1.0")
                .GET().build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    return GSON.fromJson(resp.body(), CosmeticsApiResponse.class);
                }
                System.err.println("[VoxelClient] Cosmetics API " + resp.statusCode() + " für " + uuid);
                return null;
            } catch (Exception e) {
                System.err.println("[VoxelClient] Cosmetics-Fehler: " + e.getMessage());
                return null;
            }
        }).handle((result, ex) -> {
            LOADING.remove(uuid);
            if (result != null) {
                CACHE.put(uuid, result);
                CACHED_AT.put(uuid, System.currentTimeMillis());
            }
            fireCallbacks(uuid, result);
            return null;
        });
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private static void fireCallbacks(UUID uuid, CosmeticsApiResponse result) {
        List<Consumer<CosmeticsApiResponse>> cbs = CALLBACKS.remove(uuid);
        if (cbs != null) cbs.forEach(cb -> cb.accept(result));
    }

    private static boolean isExpired(UUID uuid) {
        Long at = CACHED_AT.get(uuid);
        return at == null || (System.currentTimeMillis() - at) > CACHE_TTL_MS;
    }
}