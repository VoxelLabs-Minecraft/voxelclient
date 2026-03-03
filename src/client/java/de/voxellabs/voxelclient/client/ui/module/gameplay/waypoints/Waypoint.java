package de.voxellabs.voxelclient.client.ui.module.gameplay.waypoints;

/**
 * Repräsentiert einen einzelnen Wegpunkt.
 */
public class Waypoint {

    public String name;
    public String world;      // Welt/Dimension ID
    public double x, y, z;
    public int color;         // ARGB
    public boolean visible;
    public boolean isDeathWaypoint;

    public Waypoint() {
        // Gson-Konstruktor
    }

    public Waypoint(String name, String world, double x, double y, double z, int color) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.visible = true;
        this.isDeathWaypoint = false;
    }

    public double distanceTo(double px, double py, double pz) {
        double dx = this.x - px;
        double dy = this.y - py;
        double dz = this.z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public String getFormattedDistance(double px, double py, double pz) {
        int dist = (int) distanceTo(px, py, pz);
        if (dist >= 1000) {
            return String.format("%.1fkm", dist / 1000.0);
        }
        return dist + "m";
    }

    @Override
    public String toString() {
        return String.format("Waypoint{name='%s', world='%s', x=%.1f, y=%.1f, z=%.1f}",
                name, world, x, y, z);
    }
}
