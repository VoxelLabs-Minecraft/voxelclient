package de.voxellabs.voxelclient.client.ui.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.voxellabs.voxelclient.client.ui.module.hud.*;
import net.minecraft.client.MinecraftClient;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Draggable HUD System — VoxelClient v0.0.2
 * Ermöglicht das freie Positionieren aller HUD-Elemente per Drag & Drop.
 * Positionen werden in voxelclient/hud_positions.json gespeichert.
 *
 * Öffne den Editor mit: VoxelClient Settings → UI → HUD-Editor
 */
public class DraggableHudSystem {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path savePath;

    // HUD-Element-IDs
    public static final String KEYSTROKES = "keystrokes";
    public static final String CPS        = "cps";
    public static final String ARMOR      = "armor";
    public static final String PING       = "ping";
    public static final String HUD_FPS    = "hud_fps";
    public static final String HUD_COORDS = "hud_coords";
    public static final String HUD_DIR    = "hud_direction";
    public static final String HUD_SPEED  = "hud_speed";
    public static final String HUD_ARMOR  = "hud_armor";

    private static Map<String, int[]> positions = new HashMap<>();

    public static void register() {
        savePath = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("voxelclient").resolve("hud_positions.json");
        load();
        applyPositions();
    }

    // ─── POSITIONEN ANWENDEN ─────────────────────────────────────────────────

    public static void applyPositions() {
        applyTo(KEYSTROKES);
        applyTo(CPS);
        applyTo(ARMOR);
        applyTo(PING);
        applyTo(HUD_FPS);
        applyTo(HUD_COORDS);
        applyTo(HUD_DIR);
        applyTo(HUD_SPEED);
        applyTo(HUD_ARMOR);
    }

    private static void applyTo(String id) {
        int[] pos = positions.get(id);
        if (pos == null) return;

        switch (id) {
            case KEYSTROKES -> { KeystrokesHud.posX = pos[0]; KeystrokesHud.posY = pos[1]; }
            case CPS         -> { CpsCounter.posX = pos[0];    CpsCounter.posY = pos[1];    }
            case ARMOR       -> { ArmorDurabilityHud.posX = pos[0]; ArmorDurabilityHud.posY = pos[1]; }
            case PING        -> { PingHud.posX = pos[0];       PingHud.posY = pos[1];       }
            case HUD_FPS    -> { FPSHud.posX   = pos[0]; FPSHud.posY    = pos[1]; }
            //case HUD_COORDS -> { HudRenderer.posX_coords = pos[0]; HudRenderer.posY_coords = pos[1]; }
            case HUD_DIR    -> { DirectionHud.posX    = pos[0]; DirectionHud.posY    = pos[1]; }
            case HUD_SPEED  -> { SpeedHud.posX  = pos[0]; SpeedHud.posY  = pos[1]; }
            //case HUD_ARMOR  -> { HudRenderer.posX_armor  = pos[0]; HudRenderer.posY_armor  = pos[1]; }
        }
    }

    public static void setPosition(String id, int x, int y) {
        positions.put(id, new int[]{x, y});
        applyTo(id);
        save();
    }

    public static int[] getPosition(String id) {
        return positions.getOrDefault(id, new int[]{-1, -1});
    }

    public static void resetAll() {
        positions.clear();
        KeystrokesHud.posX = KeystrokesHud.posY = -1;
        CpsCounter.posX = CpsCounter.posY = -1;
        ArmorDurabilityHud.posX = -1; ArmorDurabilityHud.posY = -1;
        PingHud.posX = PingHud.posY = -1;
        FPSHud.posX = FPSHud.posY = -1;
        save();
    }

    // ─── PERSISTENZ ──────────────────────────────────────────────────────────

    public static void save() {
        try {
            Files.createDirectories(savePath.getParent());
            try (Writer w = Files.newBufferedWriter(savePath)) {
                GSON.toJson(positions, w);
            }
        } catch (IOException e) {
            System.err.println("[VoxelClient] Fehler beim Speichern der HUD-Positionen: " + e.getMessage());
        }
    }

    public static void load() {
        if (!Files.exists(savePath)) return;
        try (Reader r = Files.newBufferedReader(savePath)) {
            Type type = new TypeToken<Map<String, int[]>>() {}.getType();
            Map<String, int[]> loaded = GSON.fromJson(r, type);
            if (loaded != null) positions = loaded;
        } catch (IOException e) {
            System.err.println("[VoxelClient] Fehler beim Laden der HUD-Positionen: " + e.getMessage());
        }
    }
}
