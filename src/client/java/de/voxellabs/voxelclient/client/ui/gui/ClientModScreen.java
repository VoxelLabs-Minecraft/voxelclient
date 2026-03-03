package de.voxellabs.voxelclient.client.ui.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager.PlayerCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Haupt-Screen des VoxelClient-Mods.
 * Zeigt Badge-Info und alle Cosmetics des eingeloggten Spielers aus der API.
 */
public class ClientModScreen extends Screen {

    // Badge-Daten aus /api/players/:uuid
    private String badgeDisplay = null;
    private String badgeColor   = null;
    private String badgeIcon    = null;

    // Status
    private boolean loadingPlayer   = true;
    private String  errorMessage    = null;

    public ClientModScreen() {
        super(Text.literal("VoxelClient"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int bottomY = this.height - 30;

        // Schließen-Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Schließen"),
                btn -> this.close()
        ).dimensions(centerX - 60, bottomY, 120, 20).build());

        // Neu laden
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("↻ Neu laden"),
                btn -> reloadData()
        ).dimensions(centerX + 70, bottomY, 100, 20).build());

        // Beim Öffnen Daten laden
        reloadData();
    }

    private void reloadData() {
        loadingPlayer = true;
        errorMessage  = null;
        badgeDisplay  = null;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            loadingPlayer = false;
            errorMessage  = "Kein Spieler eingeloggt.";
            return;
        }

        String uuid = mc.player.getUuidAsString();

        // Badge laden
        CompletableFuture.supplyAsync(() -> fetchPlayerJson(uuid))
                .thenAcceptAsync(json -> {
                    loadingPlayer = false;
                    if (json == null) {
                        errorMessage = "API nicht erreichbar.";
                        return;
                    }
                    if (json.has("badge") && !json.get("badge").isJsonNull()) {
                        JsonObject badge = json.getAsJsonObject("badge");
                        badgeDisplay = badge.has("display") ? badge.get("display").getAsString() : "?";
                        badgeColor   = badge.has("color")   ? badge.get("color").getAsString()   : "#888888";
                        badgeIcon    = badge.has("icon")    ? badge.get("icon").getAsString()     : "✦";
                    }
                });

        // Cosmetics laden / neu laden
        CosmeticsManager.evict(uuid);
        CosmeticsManager.loadCosmetics(uuid);
    }

    private JsonObject fetchPlayerJson(String uuid) {
        try {
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create(CosmeticsManager.API_BASE + "/api/players/" + uuid))
                    .GET().header("Accept", "application/json").build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            return JsonParser.parseString(resp.body()).getAsJsonObject();
        } catch (Exception e) { return null; }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Hintergrund
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int y  = 20;

        // Titel
        ctx.drawCenteredTextWithShadow(textRenderer, "§bVoxelClient", cx, y, 0xFFFFFF);
        y += 20;

        // Fehler
        if (errorMessage != null) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§c" + errorMessage, cx, y, 0xFFFFFF);
            return;
        }

        // Badge
        y += 10;
        if (loadingPlayer) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§7Lade...", cx, y, 0xFFFFFF);
        } else if (badgeDisplay != null) {
            String colorCode = hexToMinecraft(badgeColor);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "Badge: " + colorCode + badgeIcon + " " + badgeDisplay, cx, y, 0xFFFFFF);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, "§7Kein Badge zugewiesen", cx, y, 0xFFFFFF);
        }
        y += 25;

        // Cosmetics-Überschrift
        ctx.drawCenteredTextWithShadow(textRenderer, "§eCosmetics", cx, y, 0xFFFFFF);
        y += 15;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        String uuid = mc.player.getUuidAsString();

        if (CosmeticsManager.isLoading(uuid)) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§7Cosmetics werden geladen...", cx, y, 0xFFFFFF);
            return;
        }

        PlayerCosmetics cosmetics = CosmeticsManager.getCosmetics(uuid);
        if (cosmetics == null) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§7Keine Cosmetics gefunden.", cx, y, 0xFFFFFF);
            return;
        }

        // Cape
        ctx.drawCenteredTextWithShadow(textRenderer,
                cosmeticLine("Cape", cosmetics.capeEnabled, cosmetics.capeUrl != null ? "Custom URL" : null),
                cx, y, 0xFFFFFF);
        y += 13;

        // Halo
        ctx.drawCenteredTextWithShadow(textRenderer,
                cosmeticLine("Halo", cosmetics.haloEnabled, null),
                cx, y, 0xFFFFFF);
        y += 13;

        // Wings
        ctx.drawCenteredTextWithShadow(textRenderer,
                cosmeticLine("Wings", cosmetics.wingsEnabled, null),
                cx, y, 0xFFFFFF);
        y += 13;

        // Trail
        ctx.drawCenteredTextWithShadow(textRenderer,
                cosmeticLine("Trail", cosmetics.trailEnabled, cosmetics.trailId),
                cx, y, 0xFFFFFF);
    }

    private static String cosmeticLine(String name, boolean enabled, String detail) {
        String status = enabled ? "§a✔ Aktiv" : "§7✘ Inaktiv";
        String suffix = (enabled && detail != null) ? " §8(" + detail + ")" : "";
        return "§f" + name + ": " + status + suffix;
    }

    /** Wandelt einen Hex-Farbcode (#RRGGBB) in einen nächstgelegenen Minecraft-Farbcode um. */
    private static String hexToMinecraft(String hex) {
        if (hex == null) return "§7";
        try {
            int color = Integer.parseInt(hex.replace("#", ""), 16);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8)  & 0xFF;
            int b =  color        & 0xFF;
            // Vereinfacht: Hell = weiß, mittlere Farbtöne grob zuordnen
            if (r > 200 && g < 100 && b < 100) return "§c"; // Rot
            if (r > 200 && g > 100 && b < 100) return "§6"; // Orange/Gold
            if (r > 200 && g > 200 && b < 100) return "§e"; // Gelb
            if (r < 100 && g > 150 && b < 100) return "§a"; // Grün
            if (r < 100 && g < 100 && b > 200) return "§9"; // Blau
            if (r > 150 && g < 100 && b > 200) return "§5"; // Lila
            if (r < 100 && g > 200 && b > 200) return "§b"; // Cyan
            if (r > 150 && g > 150 && b > 150) return "§f"; // Weiß
        } catch (Exception ignored) {}
        return "§7"; // Fallback: Grau
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}