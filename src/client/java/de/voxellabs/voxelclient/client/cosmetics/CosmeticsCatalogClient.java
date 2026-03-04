package de.voxellabs.voxelclient.client.cosmetics;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Lädt den globalen Cosmetics-Katalog (alle verfügbaren Items) einmalig.
 *
 * Endpoint: GET /api/cosmetics/catalog
 *
 * Nutzung:
 *   CosmeticsCatalogClient.fetch(catalog -> { ... });
 *   CosmeticsCatalog catalog = CosmeticsCatalogClient.get(); // null solange ladeend
 */
public class CosmeticsCatalogClient {

    private static final String CATALOG_URL = "https://api.voxellabs.de/api/cosmetics/catalog";
    private static final Duration TIMEOUT   = Duration.ofSeconds(10);

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private static volatile CosmeticsCatalog CACHE   = null;
    private static final AtomicBoolean       LOADING = new AtomicBoolean(false);
    private static final List<Consumer<CosmeticsCatalog>> CALLBACKS = new ArrayList<>();

    /** Gibt den gecachten Katalog zurück, oder null wenn noch nicht geladen. */
    public static CosmeticsCatalog get() {
        return CACHE;
    }

    public static boolean isLoaded() {
        return CACHE != null;
    }

    public static boolean isLoading() {
        return LOADING.get();
    }

    /**
     * Lädt den Katalog asynchron und ruft danach den Callback auf.
     * Falls bereits gecacht, wird der Callback sofort aufgerufen.
     * Mehrere Callbacks werden gesammelt und zusammen ausgeführt.
     */
    public static synchronized void fetch(Consumer<CosmeticsCatalog> callback) {
        if (CACHE != null) {
            callback.accept(CACHE);
            return;
        }
        CALLBACKS.add(callback);
        if (LOADING.compareAndSet(false, true)) {
            fetchAsync();
        }
    }

    /** Löscht den Cache (z.B. nach einem Reload). */
    public static synchronized void invalidate() {
        CACHE   = null;
        LOADING.set(false);
        CALLBACKS.clear();
    }

    private static void fetchAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CATALOG_URL))
                .timeout(TIMEOUT)
                .header("User-Agent", "VoxelClient/1.0")
                .header("Accept",     "application/json")
                .GET()
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    CosmeticsCatalog catalog = GSON.fromJson(response.body(), CosmeticsCatalog.class);
                    System.out.println("[VoxelClient] Cosmetics-Katalog geladen: "
                            + (catalog.types != null ? catalog.types.size() : 0) + " Typen");
                    return catalog;
                }
                System.err.println("[VoxelClient] Catalog API " + response.statusCode());
                return null;
            } catch (Exception e) {
                System.err.println("[VoxelClient] Catalog-Fehler: " + e.getMessage());
                return null;
            }
        }).thenAccept(result -> {
            synchronized (CosmeticsCatalogClient.class) {
                LOADING.set(false);
                if (result != null) {
                    CACHE = result;
                    List<Consumer<CosmeticsCatalog>> cbs = new ArrayList<>(CALLBACKS);
                    CALLBACKS.clear();
                    cbs.forEach(cb -> cb.accept(result));
                }
            }
        });
    }
}