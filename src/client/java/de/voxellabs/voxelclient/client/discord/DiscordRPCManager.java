package de.voxellabs.voxelclient.client.discord;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Discord Rich Presence Manager für VoxelClient.
 * <p>
 * Zeigt verschiedene Status-Informationen in Discord an:
 *   - Hauptmenü
 *   - Singleplayer
 *   - Multiplayer (Server)
 *   - Minecraft Realms
 * <p>
 * ─────────────────────────────────────────────────────────────────────────────
 * Setup:
 *   1. Discord Developer Portal: <a href="https://discord.com/developers/applications">...</a>
 *   2. Neue Application erstellen → Application ID kopieren → in APPLICATION_ID eintragen
 *   3. Unter "Rich Presence → Art Assets" folgende Bilder hochladen:
 *        - "logo"        → VoxelClient Logo (wird im RPC angezeigt)
 *        - "server"      → Server-Icon
 *        - "singleplayer"→ Singleplayer-Icon
 *        - "realms"      → Realms-Icon
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DiscordRPCManager {

    // ── Konfiguration ─────────────────────────────────────────────────────────
    /** Discord Application ID aus dem Developer Portal */
    private static final long APPLICATION_ID = 1477696449419546725L; // ← HIER ANPASSEN

    // ── State ─────────────────────────────────────────────────────────────────
    private static RandomAccessFile pipe;
    private static boolean          running      = false;
    private static Instant          sessionStart;
    private static Thread           readThread;

    private DiscordRPCManager() {}

    // ── Init ──────────────────────────────────────────────────────────────────
    public static void init() {
        sessionStart = Instant.now();

        Thread.ofVirtual().start(() -> {
            try {
                connect();
                ClientLifecycleEvents.CLIENT_STOPPING.register(c -> shutdown());
            } catch (Exception e) {
                System.err.println("[VoxelClient | Discord] Konnte nicht verbinden: " + e.getMessage());
            }
        });
    }

    private static void connect() throws Exception {
        for (int i = 0; i < 10; i++) {
            String path = getPipePath(i);
            try {
                pipe = new RandomAccessFile(path, "rw");
                System.out.println("[VoxelClient | Discord] Verbunden mit: " + path);
                break;
            } catch (FileNotFoundException ignored) {}
        }

        if (pipe == null) {
            System.err.println("[VoxelClient | Discord] Discord Pipe nicht gefunden – läuft Discord?");
            return;
        }

        // Handshake senden
        JsonObject handshake = new JsonObject();
        handshake.addProperty("v", 1);
        handshake.addProperty("client_id", String.valueOf(APPLICATION_ID));
        writeFrame(0, handshake.toString());

        running = true;

        // Antworten lesen im Hintergrund
        readThread = new Thread(() -> {
            try {
                // Einmalig auf READY warten
                readFrame();
                // Danach Pipe schließen — wir brauchen keinen dauerhaften Read
            } catch (Exception e) {
                if (running) {
                    System.err.println("[VoxelClient | Discord] Read-Fehler: " + e.getMessage());
                }
            }
        }, "discord-rpc-read");
        readThread.setDaemon(true);
        readThread.start();

        System.out.println("[VoxelClient | Discord] Handshake gesendet ✔");
        // Thread.sleep(500) und showMainMenu() komplett entfernt ← das war der Hänger
    }

    private static String getPipePath(int index) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "\\\\.\\pipe\\discord-ipc-" + index;
        }
        String[] bases = {
                System.getenv("XDG_RUNTIME_DIR"),
                System.getenv("TMPDIR"),
                System.getenv("TMP"),
                System.getenv("TEMP"),
                "/tmp"
        };
        for (String base : bases) {
            if (base != null) return base + "/discord-ipc-" + index;
        }
        return "/tmp/discord-ipc-" + index;
    }

    // ── Frame lesen ───────────────────────────────────────────────────────────
    private static void readFrame() throws IOException, InterruptedException {
        // Nicht blockieren — nur lesen wenn Daten verfügbar
        try {
            byte[] header = new byte[8];
            pipe.read(header);

            ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            buf.getInt(); // opcode
            int length = buf.getInt();

            if (length > 0 && length < 65536) {
                byte[] data = new byte[length];
                pipe.read(data);
                String json = new String(data, StandardCharsets.UTF_8);
                if (json.contains("\"READY\"")) {
                    System.out.println("[VoxelClient | Discord] Discord RPC bereit ✔");
                    // Hauptmenü erst hier setzen — wenn Discord wirklich bereit ist
                    showMainMenu();
                }
            }
        } catch (EOFException ignored) {
            Thread.sleep(100);
        }
    }

    // ── Frame schreiben ───────────────────────────────────────────────────────
    private static synchronized void writeFrame(int opcode, String json) throws IOException {
        if (pipe == null) return;
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(data.length);

        pipe.write(header.array());
        pipe.write(data);
    }

    // ── Activity senden ───────────────────────────────────────────────────────
    private static void sendActivity(JsonObject activity) {
        if (!running || pipe == null) return;
        try {
            JsonObject args = new JsonObject();
            args.addProperty("pid", (int) ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject payload = new JsonObject();
            payload.addProperty("cmd", "SET_ACTIVITY");
            payload.addProperty("nonce", UUID.randomUUID().toString());
            payload.add("args", args);

            writeFrame(1, payload.toString());
        } catch (Exception e) {
            System.err.println("[VoxelClient | Discord] sendActivity Fehler: " + e.getMessage());
        }
    }

    private static JsonObject buildActivity(String details, String state,
                                            String largeImage, String largeText,
                                            String smallImage, String smallText,
                                            Instant startTime) {
        JsonObject activity = new JsonObject();
        if (details != null) activity.addProperty("details", details);
        if (state   != null) activity.addProperty("state",   state);

        JsonObject assets = new JsonObject();
        if (largeImage != null) assets.addProperty("large_image", largeImage);
        if (largeText  != null) assets.addProperty("large_text",  largeText);
        if (smallImage != null) assets.addProperty("small_image", smallImage);
        if (smallText  != null) assets.addProperty("small_text",  smallText);
        activity.add("assets", assets);

        if (startTime != null) {
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", startTime.getEpochSecond());
            activity.add("timestamps", timestamps);
        }

        return activity;
    }

    // ── Öffentliche Methoden ──────────────────────────────────────────────────
    public static void showMainMenu() {
        sendActivity(buildActivity(
                "VoxelClient", "Im Hauptmenü",
                "logo", "VoxelClient – Fabric 1.21.4",
                null, null,
                sessionStart
        ));
    }

    public static void showSingleplayer(String worldName) {
        sendActivity(buildActivity(
                "Singleplayer", "Welt: " + (worldName != null ? worldName : "Unbekannt"),
                "singleplayer", "Singleplayer",
                "logo", "VoxelClient",
                Instant.now()
        ));
    }

    public static void showMultiplayer(String serverName, String serverAddress) {
        if (!running || pipe == null) return;

        String name = serverName    != null ? serverName    : "Server";
        String addr = serverAddress != null ? serverAddress : "";

        // Favicon asynchron laden dann Activity setzen
        Thread.ofVirtual().start(() -> {
            String favicon = fetchServerFavicon(addr);

            sendActivity(buildActivity(
                    "Multiplayer – " + name, addr,
                    favicon != null ? favicon : "logo",  // Server-Favicon oder Fallback
                    name,
                    "logo", "VoxelClient",
                    Instant.now()
            ));
        });
    }

    public static void showRealms(String realmsName) {
        sendActivity(buildActivity(
                "Minecraft Realms", realmsName != null ? realmsName : "Realm",
                "realms", "Minecraft Realms",
                "logo", "VoxelClient",
                Instant.now()
        ));
    }

    public static void clearActivity() {
        if (!running || pipe == null) return;
        try {
            JsonObject args = new JsonObject();
            args.addProperty("pid", (int) ProcessHandle.current().pid());

            JsonObject payload = new JsonObject();
            payload.addProperty("cmd", "SET_ACTIVITY");
            payload.addProperty("nonce", UUID.randomUUID().toString());
            payload.add("args", args);

            writeFrame(1, payload.toString());
        } catch (Exception e) {
            System.err.println("[VoxelClient | Discord] clearActivity Fehler: " + e.getMessage());
        }
    }

    private static String fetchServerFavicon(String serverAddress) {
        if (serverAddress == null || serverAddress.isBlank()) return null;

        String cleanAddress = serverAddress.split(":")[0].trim();
        String iconUrl = "https://api.mcsrvstat.us/icon/" + cleanAddress;

        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    java.net.URI.create(iconUrl).toURL().openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3_000);
            conn.setReadTimeout(3_000);
            conn.setRequestProperty("User-Agent", "VoxelClient-DiscordRPC/1.0");

            int status      = conn.getResponseCode();
            String contType = conn.getContentType();

            if (status == 200 && contType != null && contType.startsWith("image/")) {
                return iconUrl;
            }
        } catch (Exception e) {
            System.err.println("[VoxelClient | Discord] Favicon-Fetch fehlgeschlagen: " + e.getMessage());
        }

        return null;
    }

    public static void shutdown() {
        running = false;
        if (readThread != null) readThread.interrupt();
        try {
            clearActivity();
            if (pipe != null) pipe.close();
        } catch (Exception ignored) {}
        System.out.println("[VoxelClient | Discord] Discord RPC beendet.");
    }

    public static boolean isRunning() { return running; }
}