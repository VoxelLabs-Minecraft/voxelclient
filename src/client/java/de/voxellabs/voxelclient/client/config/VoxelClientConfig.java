package de.voxellabs.voxelclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

/**
 * Central configuration for MyClient.
 * Stored as JSON in the config directory.
 */
public class VoxelClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("voxelclient.json");

    private static VoxelClientConfig INSTANCE = new VoxelClientConfig();

    // ── HUD ──────────────────────────────────────────────────────────────────
    public boolean hudShowFps        = true;
    public boolean hudShowCoords     = true;
    public boolean hudShowArmor      = true;
    public boolean hudShowDirection  = true;
    public boolean hudShowSpeed      = false;
    public int     hudColor          = 0xFFFFFF;   // RGB, no alpha
    public boolean hudShadow         = true;
    public int     hudX              = 2;
    public int     hudY              = 2;

    // ── Zoom ─────────────────────────────────────────────────────────────────
    public boolean zoomSmoothZoom    = true;
    public double  zoomFov           = 10.0;       // degrees when fully zoomed
    public double  zoomScrollSensitivity = 1.0;   // scroll wheel adjustment

    // ── Freelook ─────────────────────────────────────────────────────────────
    public boolean freelookEnabled   = true;
    public float   freelookSensitivity = 1.0f;

    // ── Cosmetics ────────────────────────────────────────────────────────────
    public boolean capeEnabled       = false;
    public String  capeUrl           = "";
    public boolean elytraEnabled     = false;

    // ── Internal ─────────────────────────────────────────────────────────────
    private VoxelClientConfig() {}

    public static VoxelClientConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                VoxelClientConfig loaded = GSON.fromJson(reader, VoxelClientConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } catch (IOException e) {
                System.err.println("[MyClient] Failed to load config: " + e.getMessage());
            }
        } else {
            save(); // write defaults
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[MyClient] Failed to save config: " + e.getMessage());
        }
    }
}
