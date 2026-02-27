package de.voxellabs.voxelclient.client.version;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchroner Update-Checker für MyClient.
 *
 * Ablauf:
 *  1. Beim Client-Start wird {@link #checkForUpdate()} aufgerufen.
 *  2. Es wird die GitHub Releases API angefragt:
 *     GET https://api.github.com/repos/<OWNER>/<REPO>/releases/latest
 *  3. Der zurückgegebene Tag (z. B. "v1.2.0") wird mit der im Code
 *     hinterlegten {@link #CURRENT_VERSION} verglichen.
 *  4. Wenn eine neuere Version vorliegt, werden {@link #updateAvailable}
 *     und {@link #latestVersion} gesetzt.
 *  5. {@link de.voxellabs.voxelclient.client.gui.CustomMainMenuScreen} liest diese Flags
 *     aus und zeigt ein Banner an.
 *
 * ──────────────────────────────────────────────────────────────────────
 *  KONFIGURATION  →  Nur diese drei Konstanten anpassen:
 * ──────────────────────────────────────────────────────────────────────
 */
public final class VersionChecker {

    // ── Konfiguration ─────────────────────────────────────────────────────────

    /** Exakte Version, die gerade im Code steckt. Muss mit dem Git-Tag übereinstimmen. */
    public static final String CURRENT_VERSION = "0.0.1";

    /** GitHub-Nutzername / Organisation */
    private static final String GITHUB_OWNER = "VoxelLabs-Minecraft";          // ← anpassen

    /** GitHub-Repository-Name */
    private static final String GITHUB_REPO  = "voxelclient";          // ← anpassen

    /** Download-Seite, die im Banner verlinkt wird */
    public static final String DOWNLOAD_URL  =
            "https://github.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";

    // ── Status-Felder (thread-safe durch volatile) ────────────────────────────

    /** Wird auf {@code true} gesetzt, sobald eine neuere Version gefunden wurde. */
    private static volatile boolean updateAvailable = false;

    /** Tag-Name der neuesten Release, z. B. "1.2.0" (ohne führendes "v"). */
    private static volatile String latestVersion = null;

    /** Fehlermeldung, falls der Check fehlschlug (für Debug-Log). */
    private static volatile String errorMessage = null;

    /** Gibt an, ob der Check noch läuft. */
    private static volatile boolean checking = false;

    /** Gibt an, ob der Check bereits abgeschlossen ist. */
    private static volatile boolean checkDone = false;

    // ── Privater Konstruktor (nur statische Methoden) ─────────────────────────
    private VersionChecker() {}

    // ── Öffentliche API ───────────────────────────────────────────────────────

    /**
     * Startet den Update-Check asynchron. Kann mehrfach aufgerufen werden –
     * ein laufender oder bereits abgeschlossener Check wird nicht doppelt ausgeführt.
     */
    public static void checkForUpdate() {
        if (checking || checkDone) return;
        checking = true;

        System.out.println("[VoxelClient | UpdateChecker] Prüfe auf Updates... (aktuell: v" + CURRENT_VERSION + ")");

        String apiUrl = "https://api.github.com/repos/"
                + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";

        CompletableFuture.supplyAsync(() -> fetchLatestTag(apiUrl))
                .thenAccept(tag -> {
                    checking  = false;
                    checkDone = true;

                    if (tag == null) {
                        System.err.println("[VoxelClient | UpdateChecker] Check fehlgeschlagen: " + errorMessage);
                        return;
                    }

                    // Tag normalisieren: führendes "v" entfernen
                    String normalized = tag.startsWith("v") ? tag.substring(1) : tag;
                    latestVersion = normalized;

                    if (isNewer(normalized, CURRENT_VERSION)) {
                        updateAvailable = true;
                        System.out.println("[VoxelClient | UpdateChecker] ✔ Update verfügbar: v"
                                + normalized + "  (installiert: v" + CURRENT_VERSION + ")");
                    } else {
                        System.out.println("[VoxelClient | UpdateChecker] ✔ Aktuell – keine Updates.");
                    }
                });
    }

    // ── Getter ────────────────────────────────────────────────────────────────

    public static boolean isUpdateAvailable() { return updateAvailable; }
    public static String  getLatestVersion()   { return latestVersion; }
    public static boolean isCheckDone()        { return checkDone; }
    public static boolean isChecking()         { return checking; }

    // ── Interne Hilfsmethoden ─────────────────────────────────────────────────

    /**
     * Führt den HTTP-Request durch und gibt den Tag-Namen zurück,
     * oder {@code null} bei einem Fehler.
     */
    private static String fetchLatestTag(String apiUrl) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    // GitHub API verlangt einen User-Agent
                    .header("User-Agent", "VoxelClient-UpdateChecker/" + CURRENT_VERSION)
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                // Noch kein Release erstellt → kein Update
                errorMessage = "Noch keine Releases auf GitHub gefunden.";
                return null;
            }

            if (response.statusCode() != 200) {
                errorMessage = "HTTP " + response.statusCode();
                return null;
            }

            // JSON parsen
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (!json.has("tag_name")) {
                errorMessage = "Antwort enthält kein 'tag_name' Feld.";
                return null;
            }

            return json.get("tag_name").getAsString();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage = "Unterbrochen: " + e.getMessage();
            return null;
        } catch (Exception e) {
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            return null;
        }
    }

    /**
     * Vergleicht zwei SemVer-Strings (MAJOR.MINOR.PATCH).
     *
     * @param remote  Version vom Server, z. B. "1.2.0"
     * @param local   Lokal installierte Version, z. B. "1.0.0"
     * @return {@code true}, wenn {@code remote} neuer als {@code local} ist.
     */
    static boolean isNewer(String remote, String local) {
        try {
            int[] r = parseSemVer(remote);
            int[] l = parseSemVer(local);

            for (int i = 0; i < 3; i++) {
                if (r[i] > l[i]) return true;
                if (r[i] < l[i]) return false;
            }
            return false; // gleich

        } catch (Exception e) {
            // Fallback: einfacher String-Vergleich
            return !remote.equals(local);
        }
    }

    /** Parst "1.2.3" → [1, 2, 3]. Fehlende Segmente werden als 0 behandelt. */
    private static int[] parseSemVer(String version) {
        String[] parts = version.split("\\.");
        int[] nums = new int[3];
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            // Alles nach einem Bindestrich (z. B. "1.2.0-beta") ignorieren
            String segment = parts[i].split("-")[0].trim();
            nums[i] = Integer.parseInt(segment);
        }
        return nums;
    }
}
