package de.voxellabs.voxelclient.client.discord;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * Discord Rich Presence Manager für VoxelClient.
 *
 * Zeigt verschiedene Status-Informationen in Discord an:
 *   - Hauptmenü
 *   - Singleplayer
 *   - Multiplayer (Server)
 *   - Minecraft Realms
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Setup:
 *   1. Discord Developer Portal: https://discord.com/developers/applications
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

    /** Name der großen Bild-Assets im Discord Developer Portal */
    private static final String ASSET_LOGO         = "logo";
    private static final String ASSET_SERVER        = "logo";
    private static final String ASSET_SINGLEPLAYER  = "singleplayer";
    private static final String ASSET_REALMS        = "realms";

    // ── State ─────────────────────────────────────────────────────────────────
    private static Core core;
    private static boolean running = false;
    private static Instant sessionStart;
    private static Thread  callbackThread;

    // Singleton
    private DiscordRPCManager() {}

    // ── Initialisierung ───────────────────────────────────────────────────────

    /**
     * Initialisiert den Discord RPC.
     * Muss in {@code VoxelClientModClient#onInitializeClient()} aufgerufen werden.
     */
    public static void init() {
        try {
            Core.init(findNativeLibrary());

            CreateParams params = new CreateParams();
            params.setClientID(APPLICATION_ID);
            params.setFlags(CreateParams.getDefaultFlags());

            core = new Core(params);
            running = true;
            sessionStart = Instant.now();

            callbackThread = new Thread(() -> {
                while (running) {
                    try {
                        core.runCallbacks();
                        Thread.sleep(250);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception exception) {
                        System.err.println("[VoxelClient | Discord] Callback-Fehler: " + exception.getMessage());
                        break;
                    }
                }
            }, "discord-rpc-callback");
            callbackThread.setDaemon(true);
            callbackThread.start();

            System.out.println("[VoxelClient | Discord] Discord RPC initialisiert.");

            ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
        } catch (Exception exception) {
            System.err.println("[VoxelClient | Discord] Konnte Discord RPC nicht starten: " + exception.getMessage());
        }
    }

    // ── Status-Methoden ───────────────────────────────────────────────────────

    /**
     * Zeigt den Hauptmenü-Status in Discord an.
     *
     * Anzeige:
     *   🎮 VoxelClient
     *   Im Hauptmenü
     *
     * → Aufrufen in: CustomMainMenuScreen#init()
     */
    public static void showMainMenu() {
        if (!running) return;

        try (Activity activity = new Activity()) {
            activity.setType(ActivityType.PLAYING);
            activity.setDetails("VoxelClient");
            activity.setState("Im Hauptmenü");

            activity.assets().setLargeImage(ASSET_LOGO);
            activity.assets().setLargeText("VoxelClient – Fabric 1.21.4");

            activity.timestamps().setStart(sessionStart);

            core.activityManager().updateActivity(activity);
        } catch (Exception e) {
            System.err.println("[VoxelClient | Discord] showMainMenu Fehler: " + e.getMessage());
        }
    }

    /**
     * Zeigt den Singleplayer-Status in Discord an.
     *
     * Anzeige:
     *   🎮 VoxelClient
     *   Singleplayer – <Weltname>
     *
     * → Aufrufen in: GameMenuScreen#init() (wenn Singleplayer aktiv)
     *   oder über ClientPlayNetworkHandler wenn integrierter Server erkannt wird
     *
     * @param worldName Name der geladenen Welt
     */
    public static void showSingleplayer(String worldName) {
        if (!running) return;

        try (Activity activity = new Activity()) {
            activity.setType(ActivityType.PLAYING);
            activity.setDetails("Singleplayer");
            activity.setState("Welt: " + (worldName != null ? worldName : "Unbekannt"));

            activity.assets().setLargeImage(ASSET_SINGLEPLAYER);
            activity.assets().setLargeText("Singleplayer");
            activity.assets().setSmallImage(ASSET_LOGO);
            activity.assets().setSmallText("VoxelClient");

            activity.timestamps().setStart(Instant.now());

            core.activityManager().updateActivity(activity);
        } catch (Exception e) {
            System.err.println("[VoxelClient | Discord] showSingleplayer Fehler: " + e.getMessage());
        }
    }

    /**
     * Zeigt den Multiplayer-Server-Status in Discord an.
     * Das Server-Favicon wird automatisch über die mcsrvstat.us API geladen.
     *
     * @param serverName    Anzeigename des Servers
     * @param serverAddress Server-Adresse (z.B. "play.plantaria.net")
     */
    public static void showMultiplayer(String serverName, String serverAddress) {
        if (!running) return;

        // Favicon-URL asynchron ermitteln, dann RPC setzen
        String address = serverAddress != null ? serverAddress : "";

        Thread.ofVirtual().start(() -> {
            String faviconUrl = fetchServerFavicon(address);

            try (Activity activity = new Activity()) {
                activity.setType(ActivityType.PLAYING);
                activity.setDetails("Multiplayer – " + (serverName != null ? serverName : "Server"));
                activity.setState(address);

                // Großes Bild: Server-Favicon falls vorhanden, sonst Standard-Asset
                if (faviconUrl != null) {
                    activity.assets().setLargeImage(faviconUrl);
                    activity.assets().setLargeText(serverName != null ? serverName : address);
                } else {
                    activity.assets().setLargeImage(ASSET_SERVER);
                    activity.assets().setLargeText(serverName != null ? serverName : "Minecraft Server");
                }

                // Kleines Bild: immer VoxelClient Logo
                activity.assets().setSmallImage(ASSET_LOGO);
                activity.assets().setSmallText("VoxelClient");

                activity.timestamps().setStart(Instant.now());

                core.activityManager().updateActivity(activity);

            } catch (Exception e) {
                System.err.println("[VoxelClient | Discord] showMultiplayer Fehler: " + e.getMessage());
            }
        });
    }

    /**
     * Zeigt den Minecraft-Realms-Status in Discord an.
     *
     * Anzeige:
     *   🎮 VoxelClient
     *   Minecraft Realms
     *   <Realms-Name>
     *
     * → Aufrufen in: ClientPlayNetworkHandler#onGameJoin (via Mixin)
     *   wenn eine Realms-Verbindung erkannt wird (Adresse endet auf .realms.minecraft.net)
     *
     * @param realmsName Name des Realms (optional)
     */
    public static void showRealms(String realmsName) {
        if (!running) return;

        try (Activity activity = new Activity()) {
            activity.setType(ActivityType.PLAYING);
            activity.setDetails("Minecraft Realms");
            activity.setState(realmsName != null ? realmsName : "Realm");

            activity.assets().setLargeImage(ASSET_REALMS);
            activity.assets().setLargeText("Minecraft Realms");
            activity.assets().setSmallImage(ASSET_LOGO);
            activity.assets().setSmallText("VoxelClient");

            activity.timestamps().setStart(Instant.now());

            core.activityManager().updateActivity(activity);
        } catch (Exception e) {
            System.err.println("[VoxelClient | Discord] showRealms Fehler: " + e.getMessage());
        }
    }

    /**
     * Löscht die aktuelle Discord-Aktivität (z.B. beim Laden).
     */
    public static void clearActivity() {
        if (!running) return;
        try {
            core.activityManager().clearActivity();
        } catch (Exception e) {
            System.err.println("[VoxelClient | Discord] clearActivity Fehler: " + e.getMessage());
        }
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    /**
     * Beendet den Discord RPC sauber.
     * Wird automatisch beim Schließen von Minecraft aufgerufen.
     */
    public static void shutdown() {
        if (!running) return;
        running = false;

        if (callbackThread != null) {
            callbackThread.interrupt();
        }
        if (core != null) {
            try {
                core.activityManager().clearActivity();
                core.close();
            } catch (Exception e) {
                System.err.println("[VoxelClient | Discord] Shutdown-Fehler: " + e.getMessage());
            }
        }
        System.out.println("[VoxelClient | Discord] Discord RPC beendet.");
    }

    // ── Native Library ────────────────────────────────────────────────────────

    /**
     * Extrahiert die native discord_game_sdk Bibliothek aus den JAR-Ressourcen
     * und gibt den Pfad zurück.
     */
    private static File findNativeLibrary() throws IOException {
        String os   = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        String libName;
        if (os.contains("win")) {
            libName = arch.contains("64")
                    ? "discord_game_sdk.dll"
                    : "discord_game_sdk_x86.dll";
        } else if (os.contains("mac")) {
            libName = "discord_game_sdk.dylib";
        } else {
            libName = "discord_game_sdk.so";
        }

        // Aus Ressourcen extrahieren
        String resourcePath = "/natives/" + libName;
        URL resource = DiscordRPCManager.class.getResource(resourcePath);

        if (resource == null) {
            throw new IOException(
                    "Native Discord-Bibliothek nicht gefunden: " + resourcePath + "\n"
                            + "Bitte discord_game_sdk herunterladen:\n"
                            + "https://dl-game-sdk.discordapp.net/3.2.1/discord_game_sdk.zip\n"
                            + "und die Dateien nach src/main/resources/natives/ kopieren."
            );
        }

        // In temporäres Verzeichnis extrahieren
        Path tmp = Files.createTempFile("discord_game_sdk", libName);
        try (InputStream in = resource.openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        tmp.toFile().deleteOnExit();
        return tmp.toFile();
    }

    public static boolean isRunning() {
        return running;
    }

    /**
     * Fragt die mcsrvstat.us API nach dem Server-Favicon.
     *
     * Die API liefert das Favicon als PNG direkt unter:
     * https://api.mcsrvstat.us/icon/<adresse>
     *
     * Discord akzeptiert diese URL direkt als Bild-Quelle.
     *
     * @param serverAddress Server-Adresse ohne Port-Suffix
     * @return URL des Favicons oder null wenn keines gefunden
     */
    private static String fetchServerFavicon(String serverAddress) {
        if (serverAddress == null || serverAddress.isBlank()) return null;

        // Port aus Adresse entfernen (play.server.net:25565 → play.server.net)
        String cleanAddress = serverAddress.split(":")[0].trim();

        // mcsrvstat.us gibt direkt ein PNG zurück – wir prüfen nur ob der Server antwortet
        String iconUrl = "https://api.mcsrvstat.us/icon/" + cleanAddress;

        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    java.net.URI.create(iconUrl).toURL().openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3_000);
            conn.setReadTimeout(3_000);
            conn.setRequestProperty("User-Agent", "VoxelClient-DiscordRPC/1.0");

            int status = conn.getResponseCode();
            String contentType = conn.getContentType();

            // Nur zurückgeben wenn tatsächlich ein Bild geliefert wird
            if (status == 200 && contentType != null && contentType.startsWith("image/")) {
                return iconUrl;
            }

        } catch (Exception e) {
            System.err.println("[VoxelClient | Discord] Favicon-Fetch fehlgeschlagen für "
                    + cleanAddress + ": " + e.getMessage());
        }

        return null; // Kein Favicon → Fallback auf Standard-Asset
    }
}
