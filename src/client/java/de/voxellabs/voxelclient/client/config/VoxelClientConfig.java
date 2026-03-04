package de.voxellabs.voxelclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

/**
 * Central configuration for VoxelClient.
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
    public boolean hudShowCps        = true;
    public boolean hudShowKeystrokes = true;
    public boolean hudShowPing       = true;
    public int     hudColor          = 0xFFFFFF;
    public boolean hudShadow         = true;
    public int     hudX              = 2;
    public int     hudY              = 2;

    // ── Zoom ─────────────────────────────────────────────────────────────────
    public boolean zoomSmoothZoom         = true;
    public double  zoomFov                = 10.0;
    public double  zoomScrollSensitivity  = 1.0;

    // ── Freelook ─────────────────────────────────────────────────────────────
    public boolean freelookEnabled        = true;
    public float   freelookSensitivity    = 1.0f;

    // ── Cosmetics ────────────────────────────────────────────────────────────
    // Speichert die ID des aktuell aktiven Items pro Typ (0 = keins aktiv).
    // Wird beim Klick im ClientModScreen gesetzt und direkt gespeichert.
    public int cosmeticActiveCapeId   = 0;
    public int cosmeticActiveHaloId   = 0;
    public int cosmeticActiveWingsId  = 0;
    public int cosmeticActiveTrailId  = 0;

    // ── Utility ──────────────────────────────────────────────────────────────
    public boolean deathWaypoint  = true;
    public boolean chatTimestamps = true;
    public boolean uiAnimations   = true;

    // ── Internal ─────────────────────────────────────────────────────────────
    private VoxelClientConfig() {}

    public static VoxelClientConfig get() { return INSTANCE; }

    /** Gibt die aktive Item-ID für einen Typ-Namen zurück. */
    public int getActiveItemId(String typeName) {
        return switch (typeName) {
            case "cape"  -> cosmeticActiveCapeId;
            case "halo"  -> cosmeticActiveHaloId;
            case "wings" -> cosmeticActiveWingsId;
            case "trail" -> cosmeticActiveTrailId;
            default      -> 0;
        };
    }

    /** Setzt die aktive Item-ID für einen Typ-Namen. */
    public void setActiveItemId(String typeName, int itemId) {
        switch (typeName) {
            case "cape"  -> cosmeticActiveCapeId   = itemId;
            case "halo"  -> cosmeticActiveHaloId   = itemId;
            case "wings" -> cosmeticActiveWingsId  = itemId;
            case "trail" -> cosmeticActiveTrailId  = itemId;
        }
    }

    /** Toggelt ein Item: aktiviert es, oder deaktiviert es wenn es bereits aktiv ist. */
    public void toggleItem(String typeName, int itemId) {
        int current = getActiveItemId(typeName);
        setActiveItemId(typeName, current == itemId ? 0 : itemId);
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                VoxelClientConfig loaded = GSON.fromJson(reader, VoxelClientConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } catch (IOException e) {
                System.err.println("[VoxelClient] Failed to load config: " + e.getMessage());
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[VoxelClient] Failed to save config: " + e.getMessage());
        }
    }
}