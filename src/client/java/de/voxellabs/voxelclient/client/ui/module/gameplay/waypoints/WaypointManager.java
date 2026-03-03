package de.voxellabs.voxelclient.client.ui.module.gameplay.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Waypoint System — VoxelClient v0.0.2
 * Speichert, ladet und rendert Wegpunkte pro Dimension.
 * Wegpunkte werden in .minecraft/voxelclient/waypoints.json gespeichert.
 */
public class WaypointManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Waypoint> waypoints = new ArrayList<>();
    private static Path savePath;

    // HUD-Einstellungen
    public static boolean showHudLabels = true;
    public static int maxRenderDistance = 5000; // Meter

    public static void register() {
        // Speicherpfad ermitteln
        Path configDir = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("voxelclient");
        savePath = configDir.resolve("waypoints.json");

        load();
        HudRenderCallback.EVENT.register(WaypointManager::renderHud);
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    public static void addWaypoint(Waypoint wp) {
        waypoints.add(wp);
        save();
    }

    public static void removeWaypoint(Waypoint wp) {
        waypoints.remove(wp);
        save();
    }

    public static void removeWaypointByName(String name) {
        waypoints.removeIf(wp -> wp.name.equals(name));
        save();
    }

    public static List<Waypoint> getAll() {
        return new ArrayList<>(waypoints);
    }

    public static List<Waypoint> getForCurrentWorld() {
        String world = getCurrentWorld();
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : waypoints) {
            if (world.equals(wp.world)) result.add(wp);
        }
        return result;
    }

    // ─── PERSISTENZ ──────────────────────────────────────────────────────────

    public static void save() {
        try {
            Files.createDirectories(savePath.getParent());
            try (Writer writer = Files.newBufferedWriter(savePath)) {
                GSON.toJson(waypoints, writer);
            }
        } catch (IOException e) {
            System.err.println("[VoxelClient] Fehler beim Speichern der Wegpunkte: " + e.getMessage());
        }
    }

    public static void load() {
        if (!Files.exists(savePath)) return;
        try (Reader reader = Files.newBufferedReader(savePath)) {
            Type type = new TypeToken<List<Waypoint>>() {}.getType();
            List<Waypoint> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                waypoints.clear();
                waypoints.addAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("[VoxelClient] Fehler beim Laden der Wegpunkte: " + e.getMessage());
        }
    }

    // ─── HUD RENDERING ───────────────────────────────────────────────────────

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!showHudLabels) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        String world = getCurrentWorld();
        Vec3d playerPos = client.player.getPos();

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int centerX = screenW / 2;
        int centerY = screenH / 2;

        for (Waypoint wp : waypoints) {
            if (!wp.visible || !world.equals(wp.world)) continue;

            double dist = wp.distanceTo(playerPos.x, playerPos.y, playerPos.z);
            if (dist > maxRenderDistance) continue;

            // Richtungsanzeige im HUD (Kompass-Stil)
            renderWaypointIndicator(context, client, wp, playerPos, screenW, screenH);
        }
    }

    /**
     * Rendert einen Richtungspfeil/Indikator am Bildschirmrand für den Wegpunkt.
     */
    private static void renderWaypointIndicator(DrawContext context, MinecraftClient client,
                                                  Waypoint wp, Vec3d playerPos,
                                                  int screenW, int screenH) {
        double dx = wp.x - playerPos.x;
        double dz = wp.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 2) return;

        float playerYaw = client.player.getYaw();
        double angleToWp = Math.toDegrees(Math.atan2(dz, dx)) - 90;
        double relAngle = ((angleToWp - playerYaw) % 360 + 360) % 360;
        if (relAngle > 180) relAngle -= 360;

        // Begrenze auf Bildschirmrand
        int edgeMargin = 12;
        int maxX = screenW / 2 - edgeMargin;

        double clampedAngle = Math.max(-80, Math.min(80, relAngle));
        int indicatorX = (int)(screenW / 2 + (clampedAngle / 80.0) * maxX);
        int indicatorY = screenH / 2 - 60; // Über Kreuzfahrenlinie

        // Farbe & Beschriftung
        int color = wp.color | 0xFF000000;
        String label = wp.name + " §7" + wp.getFormattedDistance(playerPos.x, playerPos.y, playerPos.z);
        int textW = client.textRenderer.getWidth(label);

        context.fill(indicatorX - 1, indicatorY - 1, indicatorX + 2, indicatorY + 6, color);
        context.drawText(client.textRenderer, label,
                indicatorX - textW / 2, indicatorY - 11, color, true);
    }

    // ─── HILFSMETHODEN ───────────────────────────────────────────────────────

    public static String getCurrentWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return "unknown";
        Identifier dim = client.world.getRegistryKey().getValue();
        // Server-spezifisch: IP + Dimension
        String serverStr = "";
        if (client.getCurrentServerEntry() != null) {
            serverStr = client.getCurrentServerEntry().address + "_";
        }
        return serverStr + dim.toString();
    }
}
