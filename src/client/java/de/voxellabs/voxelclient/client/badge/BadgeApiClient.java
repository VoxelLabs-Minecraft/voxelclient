package de.voxellabs.voxelclient.client.badge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.voxellabs.voxelclient.client.utils.VoxelClientNetwork;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BadgeApiClient {

    public static final String API_BASE_URL = "https://api.voxellabs.de";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    // ── CachedBadge als normale Klasse statt Record ───────────────────────────
    public static class CachedBadge {
        public final String name;
        public final String display;
        public final String color;
        public final String icon;
        final long fetchedAt;

        CachedBadge(String name, String display, String color, String icon, long fetchedAt) {
            this.name      = name;
            this.display   = display;
            this.color     = color;
            this.icon      = icon;
            this.fetchedAt = fetchedAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS;
        }
    }

    // Sentinel: "bekannt aber kein Badge" — kein Record mehr nötig
    private static final CachedBadge NO_BADGE =
            new CachedBadge(null, null, null, null, 0L);

    private static final Map<UUID, CachedBadge> CACHE = new ConcurrentHashMap<>();

    private BadgeApiClient() {}

    public static CachedBadge getBadge(UUID uuid) {
        // Nur abfragen, wenn Spieler VoxelClient nutzt
        if (!VoxelClientNetwork.isVoxelUser(uuid)) return null;

        CachedBadge cached = CACHE.get(uuid);
        if (cached == null || cached.isExpired()) {
            Thread.ofVirtual().start(() -> fetchAndCache(uuid));
            return (cached == null || cached == NO_BADGE) ? null : cached;
        }
        return cached == NO_BADGE ? null : cached;
    }

    public static String getBadgeString(UUID uuid) {
        CachedBadge badge = getBadge(uuid);
        if (badge == null) return "§7✦ §r";
        return formatColorCode(badge.color) + (badge.icon != null ? badge.icon : "✦") + " §r";
    }

    private static void fetchAndCache(UUID uuid) {
        try {
            String uuidStr = uuid.toString().toLowerCase();

            HttpURLConnection conn = (HttpURLConnection)
                    URI.create(API_BASE_URL + "/api/players/" + uuidStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4_000);
            conn.setReadTimeout(4_000);
            conn.setRequestProperty("User-Agent", "VoxelClient-Mod/1.0");

            int status = conn.getResponseCode();

            if (status != 200) {
                CACHE.put(uuid, NO_BADGE);
                return;
            }

            JsonObject json;
            try (InputStreamReader reader = new InputStreamReader(
                    conn.getInputStream(), StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }

            if (!json.has("badge_name") || json.get("badge_name").isJsonNull()) {
                CACHE.put(uuid, NO_BADGE);
                return;
            }

            String badgeName = getStr(json, "badge_name");
            String color     = getStr(json, "color");

            CACHE.put(uuid, new CachedBadge(
                    badgeName,
                    getStr(json, "display"),
                    color,
                    getStr(json, "icon"),
                    System.currentTimeMillis()
            ));

        } catch (Exception e) {
            if (CreatorList.isCreator(uuid)) {
                CACHE.put(uuid, new CachedBadge(
                        "creator", "Creator", "#CC2200", "✦",
                        System.currentTimeMillis()));
            } else {
                CACHE.put(uuid, NO_BADGE);
            }
        }
    }

    private static String formatColorCode(String hex) {
        if (hex == null) return "§7";
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);

            if (r > 180 && g < 80  && b < 80)  return "§4";
            if (r > 220 && g < 120 && b < 80)  return "§c";
            if (r > 200 && g > 120 && b < 60)  return "§6";
            if (r > 200 && g > 200 && b < 80)  return "§e";
            if (r < 80  && g > 180 && b < 80)  return "§2";
            if (r < 120 && g > 200 && b < 120) return "§a";
            if (r < 80  && g < 80  && b > 180) return "§1";
            if (r < 120 && g < 120 && b > 220) return "§9";
            if (r > 100 && g < 80  && b > 180) return "§5";
            if (r > 180 && g < 120 && b > 200) return "§d";
            if (r < 80  && g > 180 && b > 180) return "§3";
            if (r > 160 && g > 200 && b > 200) return "§b";
        } catch (Exception ignored) {}
        return "§7";
    }

    private static String getStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString() : null;
    }
}
