package de.voxellabs.voxelclient.client.cosmetics.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lädt PNG-Texturen von einer URL und registriert sie als Minecraft-Textur.
 * Die Textur-Daten liegen vollständig auf dem Server — keine lokalen Dateien nötig.
 */
public class WebTextureLoader {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // URL → registrierter Minecraft-Identifier
    private static final Map<String, Identifier> LOADED  = new ConcurrentHashMap<>();
    private static final Map<String, Boolean>    LOADING = new ConcurrentHashMap<>();

    /**
     * Gibt den Identifier für eine URL zurück.
     * Gibt null zurück solange die Textur noch heruntergeladen wird.
     */
    public static Identifier getOrLoad(String url, String cacheKey) {
        if (url == null) return null;
        if (LOADED.containsKey(url)) return LOADED.get(url);
        if (!LOADING.containsKey(url)) {
            LOADING.put(url, true);
            loadAsync(url, cacheKey);
        }
        return null;
    }

    public static void clearCache() {
        LOADED.clear();
        LOADING.clear();
    }

    private static void loadAsync(String url, String cacheKey) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "VoxelClient/1.0")
                .GET()
                .build();

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    return HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                } catch (Exception e) {
                    System.err.println("[VoxelClient] Textur-Download fehlgeschlagen: " + url);
                    return null;
                }
            })
            .thenAccept(response -> {
                if (response == null || response.statusCode() != 200) {
                    LOADING.remove(url);
                    return;
                }

                // Texturen müssen auf dem Main-Thread registriert werden
                MinecraftClient.getInstance().execute(() -> {
                    try (InputStream stream = response.body()) {
                        NativeImage image = NativeImage.read(stream);
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

                        Identifier id = Identifier.of("voxelclient", "dynamic/" + cacheKey
                                .replaceAll("[^a-z0-9_.-]", "_").toLowerCase());

                        MinecraftClient.getInstance()
                                .getTextureManager()
                                .registerTexture(id, texture);

                        LOADED.put(url, id);
                        LOADING.remove(url);

                    } catch (Exception e) {
                        System.err.println("[VoxelClient] Textur-Fehler (" + url + "): " + e.getMessage());
                        LOADING.remove(url);
                    }
                });
            });
    }
}
