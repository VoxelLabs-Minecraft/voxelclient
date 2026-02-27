package de.voxellabs.voxelclient.client.cosmetics;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Manages custom cosmetics (Cape, Elytra texture).
 *
 * Usage:
 *   CosmeticsManager.loadCape("https://example.com/mycape.png");
 *   Identifier tex = CosmeticsManager.getCapeTexture();
 */
public class CosmeticsManager {

    private static final Identifier CAPE_ID = Identifier.of("myclient", "dynamic_cape");
    private static boolean capeLoaded = false;
    private static boolean capeLoading = false;

    // ── Cape Loading ─────────────────────────────────────────────────────────

    /** Starts an async load of the cape from the configured URL. */
    public static void reloadCapeFromConfig() {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        if (cfg.capeEnabled && !cfg.capeUrl.isEmpty()) {
            loadCape(cfg.capeUrl);
        } else {
            unloadCape();
        }
    }

    public static void loadCape(String url) {
        if (capeLoading) return;
        capeLoading = true;
        capeLoaded  = false;

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient http = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
                HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());

                if (resp.statusCode() != 200) {
                    System.err.println("[MyClient] Cape download failed: HTTP " + resp.statusCode());
                    return null;
                }
                return NativeImage.read(resp.body());
            } catch (Exception e) {
                System.err.println("[MyClient] Cape download error: " + e.getMessage());
                return null;
            }
        }).thenAcceptAsync(image -> {
            capeLoading = false;
            if (image == null) return;

            // Register texture on the render thread
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                mc.getTextureManager().registerTexture(CAPE_ID, texture);
                capeLoaded = true;
                System.out.println("[MyClient] Cape loaded successfully.");
            });
        });
    }

    public static void unloadCape() {
        capeLoaded  = false;
        capeLoading = false;
        MinecraftClient.getInstance().execute(() ->
            MinecraftClient.getInstance().getTextureManager().destroyTexture(CAPE_ID)
        );
    }

    /** Returns the cape texture identifier, or null if no cape is loaded. */
    public static Identifier getCapeTexture() {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        return (cfg.capeEnabled && capeLoaded) ? CAPE_ID : null;
    }

    public static boolean isCapeLoaded()   { return capeLoaded; }
    public static boolean isCapeLoading()  { return capeLoading; }
}
