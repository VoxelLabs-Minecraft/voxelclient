package de.voxellabs.voxelclient.client.cosmetics;

import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CosmeticsApiClient {

    private static final String BASE_URL = "https://api.voxellabs.de/api/cosmetics/";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long RATE_LIMIT_BACKOFF_MS = 60_000;

    private static final Map<UUID, CosmeticsApiResponse>             CACHE        = new ConcurrentHashMap<>();
    private static final Set<UUID>                                    LOADING      = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long>                              RATE_LIMITED = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Consumer<CosmeticsApiResponse>>> CALLBACKS = new ConcurrentHashMap<>();

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private static final Gson GSON = new Gson();

    public static CosmeticsApiResponse getCosmetics(UUID uuid) {
        if (CACHE.containsKey(uuid)) return CACHE.get(uuid);
        prefetch(uuid);
        return null;
    }

    public static boolean isCached(UUID uuid) {
        return CACHE.containsKey(uuid);
    }

    public static void prefetch(UUID uuid) {
        if (CACHE.containsKey(uuid)) return;

        // 429-Backoff aktiv?
        Long blockedUntil = RATE_LIMITED.get(uuid);
        if (blockedUntil != null && System.currentTimeMillis() < blockedUntil) return;

        // LOADING.add() ist atomar – gibt false zurück wenn UUID bereits drin
        if (!LOADING.add(uuid)) return;

        fetchAsync(uuid);
    }

    public static void fetchWithCallback(UUID uuid, Consumer<CosmeticsApiResponse> callback) {
        CosmeticsApiResponse cached = CACHE.get(uuid);
        if (cached != null) { callback.accept(cached); return; }
        CALLBACKS.computeIfAbsent(uuid, k -> new ArrayList<>()).add(callback);
        prefetch(uuid);
    }

    public static void invalidate(UUID uuid) {
        CACHE.remove(uuid);
        LOADING.remove(uuid);
        RATE_LIMITED.remove(uuid);
        CALLBACKS.remove(uuid);
    }

    public static void clearCache() {
        CACHE.clear();
        LOADING.clear();
        RATE_LIMITED.clear();
        CALLBACKS.clear();
    }

    /**
     * Lädt Cosmetics für alle aktuell sichtbaren Spieler auf einmal.
     * Nur einmalig aufrufen (z.B. beim Screen-Öffnen).
     */
    public static void loadAllVisible(MinecraftClient mc) {
        if (mc.world == null) return;
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            prefetch(player.getUuid());
        }
    }

    private static void fetchAsync(UUID uuid) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + uuid))
                .timeout(TIMEOUT)
                .header("User-Agent", "VoxelClient/1.0")
                .header("Accept", "application/json")
                .GET()
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return GSON.fromJson(response.body(), CosmeticsApiResponse.class);
                }
                if (response.statusCode() == 429) {
                    RATE_LIMITED.put(uuid, System.currentTimeMillis() + RATE_LIMIT_BACKOFF_MS);
                    System.err.println("[VoxelClient] Rate-Limited für " + uuid + " – Retry in 60s");
                } else {
                    System.err.println("[VoxelClient] Cosmetics API " + response.statusCode() + " für " + uuid);
                }
                return null;
            } catch (Exception e) {
                System.err.println("[VoxelClient] Cosmetics-API Fehler: " + e.getMessage());
                return null;
            }
        }).thenAccept(result -> {
            LOADING.remove(uuid);
            if (result != null) {
                CACHE.put(uuid, result);
                List<Consumer<CosmeticsApiResponse>> cbs = CALLBACKS.remove(uuid);
                if (cbs != null) cbs.forEach(cb -> cb.accept(result));
            }
        });
    }
}