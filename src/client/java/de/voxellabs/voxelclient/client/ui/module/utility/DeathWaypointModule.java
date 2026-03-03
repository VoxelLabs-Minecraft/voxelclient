package de.voxellabs.voxelclient.client.ui.module.utility;

import de.voxellabs.voxelclient.client.ui.module.gameplay.waypoints.Waypoint;
import de.voxellabs.voxelclient.client.ui.module.gameplay.waypoints.WaypointManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Death Waypoint — VoxelClient v0.0.2
 * Speichert automatisch einen Wegpunkt beim Tod des Spielers.
 * Zeigt eine kurze Benachrichtigung im Chat an.
 */
public class DeathWaypointModule {

    public static boolean enabled = true;
    private static boolean wasDead = false;

    // Maximale gespeicherte Death-Waypoints
    private static final int MAX_DEATH_WAYPOINTS = 10;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(DeathWaypointModule::onTick);
    }

    private static void onTick(MinecraftClient client) {
        if (!enabled) return;
        if (client.player == null) {
            wasDead = false;
            return;
        }

        boolean isDead = client.player.isDead() || client.player.getHealth() <= 0;

        if (isDead && !wasDead) {
            // Spieler ist gerade gestorben — Wegpunkt speichern
            saveDeathWaypoint(client);
        }

        wasDead = isDead;
    }

    private static void saveDeathWaypoint(MinecraftClient client) {
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        String world = WaypointManager.getCurrentWorld();
        String time = LocalDateTime.now().format(TIME_FORMAT);

        // Alte Death-Waypoints zählen und bei Bedarf älteste löschen
        long deathCount = WaypointManager.getAll().stream()
                .filter(wp -> wp.isDeathWaypoint)
                .count();

        if (deathCount >= MAX_DEATH_WAYPOINTS) {
            WaypointManager.getAll().stream()
                    .filter(wp -> wp.isDeathWaypoint)
                    .findFirst()
                    .ifPresent(WaypointManager::removeWaypoint);
        }

        Waypoint deathWp = new Waypoint(
                "Tod " + time,
                world,
                x, y, z,
                0xFF3333 // Rot
        );
        deathWp.isDeathWaypoint = true;

        WaypointManager.addWaypoint(deathWp);

        // Chat-Benachrichtigung
        client.player.sendMessage(
                Text.literal("§c☠ §7Tode-Wegpunkt gespeichert: §f" +
                        String.format("%.0f, %.0f, %.0f", x, y, z)),
                false
        );
    }
}
