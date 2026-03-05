package de.voxellabs.voxelclient.client.ui.gui;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiResponse;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsCatalog;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsCatalogClient;
import de.voxellabs.voxelclient.client.cosmetics.utility.WebTextureLoader;
import de.voxellabs.voxelclient.client.ui.hud.HudEditorScreen;
import de.voxellabs.voxelclient.client.version.VersionChecker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientModScreen extends Screen {

    // ── Farben ────────────────────────────────────────────────────────────────
    private static final int COL_BG          = 0xFF07070F;
    private static final int COL_SIDEBAR     = 0xFF0D0D1A;
    private static final int COL_PANEL       = 0xFF10101E;
    private static final int COL_BORDER      = 0xFF1E1E3A;
    private static final int COL_ACCENT      = 0xFF4A6CF7;
    private static final int COL_SELECTED_BG = 0xFF1A1A35;
    private static final int COL_HOVER_BG    = 0xFF141428;
    private static final int COL_TEXT        = 0xFFE0E0FF;
    private static final int COL_TEXT_DIM    = 0xFF6B6B9A;
    private static final int COL_TEXT_LABEL  = 0xFF9090BB;
    private static final int COL_ON          = 0xFF4A6CF7;
    private static final int COL_OFF         = 0xFF2A2A4A;
    private static final int COL_TITLE_A     = 0xFF6AB4FF;
    private static final int COL_LOCKED_BG   = 0xFF0C0C18;
    private static final int COL_LOCKED_TEXT = 0xFF3A3A5A;
    private static final int COL_ACTIVE_GLOW = 0xFF1A2A5A;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int SIDEBAR_W = 160;
    private static final int PADDING   = 16;
    private static final int CAT_H     = 36;
    private static final int ROW_H     = 34;
    private static final int TOGGLE_W  = 36;
    private static final int TOGGLE_H  = 18;
    private static final int HEADER_H  = 50;

    // ── Cosmetics Layout ──────────────────────────────────────────────────────
    private static final int SUBTAB_H     = 30;
    private static final int SUBTAB_GAP   = 4;
    private static final int CARD_W       = 120;
    private static final int CARD_H       = 128;
    private static final int CARD_PREVIEW = 64;
    private static final int CARD_GAP     = 8;

    private static final Identifier LOGO_TEXTURE =
            Identifier.of("voxelclient", "icons/icon.png");

    private static final String[] COSMETIC_TABS       = { "Cape", "Halo", "Wings", "Trail", "Badges" };
    private static final String[] COSMETIC_TYPE_NAMES = { "cape", "halo", "wings", "trail", "badge" };
    private static final String[] COSMETIC_ICONS      = { "🧣", "✨", "🪽", "✦", "✦" };
    private int selectedCosmeticTab = 0;

    // ── Hauptkategorien ───────────────────────────────────────────────────────
    private static final String[] CATEGORIES = { "Cosmetics", "HUD", "Visual", "Utility" };
    private static final String[] CAT_ICONS  = { "✦", "◈", "◉", "⚙" };
    private int selectedCategory = 0;

    // ── Badge ─────────────────────────────────────────────────────────────────
    private String  badgeDisplay = null;
    private String  badgeColor   = null;
    private String  badgeIcon    = null;
    private boolean loadingBadge = true;

    // ── Cosmetics Ladestatus ──────────────────────────────────────────────────
    // true sobald der async Fetch fertig ist — verhindert "Gesperrt" während des Ladens
    private volatile boolean playerDataReady = false;

    // ── Alle Badges (public endpoint) ────────────────────────────────────────
    private static class BadgeInfo {
        int    id;
        String name, display, color, icon;
    }
    private final List<BadgeInfo> allBadges    = new ArrayList<>();
    private volatile boolean      badgesLoading = true;

    // Badges die der eigene Spieler besitzt (aus /api/players/:uuid/badges)
    private final List<Integer>   ownedBadgeIds  = new ArrayList<>();
    private volatile boolean      ownedBadgesLoading = true;

    public ClientModScreen() {
        super(Text.literal("VoxelClient"));
    }

    @Override
    protected void init() {
        // Tab-Index zurücksetzen damit keine ArrayIndexOutOfBoundsException auftreten kann
        selectedCosmeticTab = 0;
        loadBadge();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            UUID playerUuid = mc.player.getUuid();

            // Katalog laden (meist bereits gecacht vom Mod-Start)
            CosmeticsCatalogClient.fetch(catalog -> { /* gecacht */ });

            // Spieler-Daten laden. Callback setzt playerDataReady=true sobald fertig
            // (auch bei Fehler/null, damit kein ewiger Ladeindikator entsteht).
            CosmeticsApiClient.fetchWithCallback(playerUuid, data -> {
                playerDataReady = true;
            });
        } else {
            // Hauptmenü: kein Spieler → sofort fertig, alles als gesperrt anzeigen
            playerDataReady = true;
            CosmeticsCatalogClient.fetch(catalog -> { /* gecacht */ });
        }
        // Alle verfügbaren Badges laden (public)
        loadAllBadges();
        // Besessene Badges des eigenen Spielers laden
        loadOwnedBadges();
    }

    private void loadOwnedBadges() {
        ownedBadgesLoading = true;
        ownedBadgeIds.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { ownedBadgesLoading = false; return; }
        String uuid = mc.player.getUuid().toString();
        Thread.ofVirtual().start(() -> {
            try {
                java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(5)).build();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://api.voxellabs.de/api/players/" + uuid))
                        .header("User-Agent", "VoxelClient/1.0")
                        .GET().build();
                java.net.http.HttpResponse<String> resp =
                        http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    com.google.gson.JsonObject json =
                            new Gson().fromJson(resp.body(), com.google.gson.JsonObject.class);
                    if (json.has("owned_badge_ids")) {
                        com.google.gson.JsonArray arr = json.getAsJsonArray("owned_badge_ids");
                        synchronized (ownedBadgeIds) {
                            ownedBadgeIds.clear();
                            arr.forEach(el -> ownedBadgeIds.add(el.getAsInt()));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[VoxelClient] Owned-Badges Fehler: " + e.getMessage());
            } finally {
                ownedBadgesLoading = false;
            }
        });
    }

    private void loadAllBadges() {
        badgesLoading = true;
        allBadges.clear();
        Thread.ofVirtual().start(() -> {
            try {
                HttpClient http = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5)).build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.voxellabs.de/api/badges/public"))
                        .header("User-Agent", "VoxelClient/1.0")
                        .GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonArray arr = new Gson().fromJson(resp.body(), JsonArray.class);
                    for (var el : arr) {
                        JsonObject o = el.getAsJsonObject();
                        BadgeInfo b  = new BadgeInfo();
                        b.id      = o.get("id").getAsInt();
                        b.name    = o.get("name").getAsString();
                        b.display = o.get("display").getAsString();
                        b.color   = o.get("color").getAsString();
                        b.icon    = o.get("icon").getAsString();
                        allBadges.add(b);
                        // Katalog befüllen damit Mixins per ID nachschlagen können
                        BadgeApiClient.putCatalogBadge(b.id, b.name, b.display, b.color, b.icon);
                    }
                }
            } catch (Exception e) {
                System.err.println("[VoxelClient] Badges laden fehlgeschlagen: " + e.getMessage());
            } finally {
                badgesLoading = false;
            }
        });
    }

    private void loadBadge() {
        loadingBadge = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            // Hauptmenü: kein Spieler, kein Badge
            loadingBadge = false;
            badgeDisplay = null;
            return;
        }
        UUID uuid = mc.player.getUuid();
        BadgeApiClient.fetchWithCallback(uuid, badge -> {
            loadingBadge = false;
            if (badge == null) return;
            badgeDisplay = badge.display;
            badgeColor   = badge.color;
            badgeIcon    = badge.icon;
        });
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COL_BG);
        drawSidebar(ctx, mouseX, mouseY);
        drawContentPanel(ctx, mouseX, mouseY);
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private void drawSidebar(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(0, 0, SIDEBAR_W, height, COL_SIDEBAR);
        ctx.fill(SIDEBAR_W - 1, 0, SIDEBAR_W, height, COL_BORDER);

        int logoY = 14;
        // Logo-Textur (assets/voxelclient/icons/icon.png)
        ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                LOGO_TEXTURE, PADDING, logoY, 0, 0, 32, 32, 32, 32);
        ctx.drawTextWithShadow(textRenderer, "§bVoxel§5Client", PADDING + 36, logoY + 6, COL_TEXT);
        ctx.drawTextWithShadow(textRenderer, "§7v" + VersionChecker.CURRENT_VERSION,
                PADDING + 36, logoY + 17, COL_TEXT_DIM);
        ctx.fill(PADDING, logoY + 40, SIDEBAR_W - PADDING, logoY + 41, COL_BORDER);

        int playerY = logoY + 48;
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = mc.player != null ? mc.player.getName().getString() : "Nicht eingeloggt";
        ctx.drawTextWithShadow(textRenderer, "§f" + name, PADDING, playerY, COL_TEXT);

        // Eigenes Badge: activeBadgeId aus Config → Katalog-Lookup
        String badgeStr;
        int activeId = VoxelClientConfig.get().activeBadgeId;
        if (loadingBadge) {
            badgeStr = "§7Lädt...";
        } else if (activeId != 0) {
            BadgeApiClient.CachedBadge activeBadge = BadgeApiClient.getBadgeById(activeId);
            if (activeBadge != null) {
                badgeStr = BadgeApiClient.formatColor(activeBadge.color)
                        + (activeBadge.icon != null ? activeBadge.icon : "✦")
                        + " " + activeBadge.display;
            } else {
                // Katalog noch nicht geladen — Fallback auf Server-Badge
                badgeStr = badgeDisplay != null
                        ? hexToMc(badgeColor) + (badgeIcon != null ? badgeIcon : "✦") + " " + badgeDisplay
                        : "§7Kein Badge";
            }
        } else {
            badgeStr = "§7Kein Badge";
        }
        ctx.drawTextWithShadow(textRenderer, badgeStr, PADDING, playerY + 12, COL_TEXT_DIM);
        ctx.fill(PADDING, playerY + 26, SIDEBAR_W - PADDING, playerY + 27, COL_BORDER);

        int catStartY = playerY + 50;
        ctx.drawTextWithShadow(textRenderer, "§7MENU", PADDING, catStartY - 10, COL_TEXT_DIM);

        for (int i = 0; i < CATEGORIES.length; i++) {
            int cy = catStartY + i * (CAT_H + 2);
            boolean selected = selectedCategory == i;
            boolean hovered  = mouseX >= 4 && mouseX < SIDEBAR_W - 4
                    && mouseY >= cy && mouseY < cy + CAT_H;

            if (selected)     ctx.fill(4, cy, SIDEBAR_W - 4, cy + CAT_H, COL_SELECTED_BG);
            else if (hovered) ctx.fill(4, cy, SIDEBAR_W - 4, cy + CAT_H, COL_HOVER_BG);
            if (selected)     ctx.fill(4, cy + 6, 7, cy + CAT_H - 6, COL_ACCENT);

            int tc = selected ? COL_TEXT : (hovered ? 0xFFCCCCFF : COL_TEXT_LABEL);
            ctx.drawTextWithShadow(textRenderer,
                    CAT_ICONS[i] + "  " + CATEGORIES[i], PADDING + 6, cy + 14, tc);
        }

        // Version wird jetzt neben dem Logo angezeigt
    }

    // ── Content Panel ─────────────────────────────────────────────────────────

    private void drawContentPanel(DrawContext ctx, int mouseX, int mouseY) {
        int x = SIDEBAR_W, w = width - SIDEBAR_W;

        ctx.fill(x, 0, x + w, HEADER_H, COL_SIDEBAR);
        ctx.fill(x, HEADER_H - 1, x + w, HEADER_H, COL_BORDER);
        ctx.drawTextWithShadow(textRenderer,
                "§b" + CAT_ICONS[selectedCategory] + "  " + CATEGORIES[selectedCategory],
                x + PADDING, 14, COL_TITLE_A);
        ctx.drawTextWithShadow(textRenderer, "§7Einstellungen", x + PADDING, 28, COL_TEXT_DIM);

        int cx2 = x + w - 24, cy2 = 15;
        boolean ch = mouseX >= cx2 && mouseX <= cx2 + 18 && mouseY >= cy2 && mouseY <= cy2 + 18;
        ctx.fill(cx2, cy2, cx2 + 18, cy2 + 18, ch ? 0xFF3A1A2A : COL_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, "✕", cx2 + 9, cy2 + 5,
                ch ? 0xFFFF6B6B : COL_TEXT_DIM);

        int cx = x + PADDING, cy = HEADER_H + PADDING, cw = w - PADDING * 2;
        switch (selectedCategory) {
            case 0 -> drawCosmetics(ctx, mouseX, mouseY, cx, cy, cw);
            case 1 -> drawHud(ctx, mouseX, mouseY, cx, cy, cw);
            case 2 -> drawVisual(ctx, mouseX, mouseY, cx, cy, cw);
            case 3 -> drawUtility(ctx, mouseX, mouseY, cx, cy, cw);
        }
    }

    // ── Cosmetics ─────────────────────────────────────────────────────────────

    private void drawCosmetics(DrawContext ctx, int mx, int my, int x, int y, int w) {
        // Sicherstellen dass der Tab-Index immer im gültigen Bereich liegt
        if (selectedCosmeticTab < 0 || selectedCosmeticTab >= COSMETIC_TABS.length) {
            selectedCosmeticTab = 0;
        }
        // Sub-Tab-Leiste
        int tabW = (w - (COSMETIC_TABS.length - 1) * SUBTAB_GAP) / COSMETIC_TABS.length;
        for (int i = 0; i < COSMETIC_TABS.length; i++) {
            int tx  = x + i * (tabW + SUBTAB_GAP);
            boolean sel = selectedCosmeticTab == i;
            boolean hov = mx >= tx && mx < tx + tabW && my >= y && my < y + SUBTAB_H;
            ctx.fill(tx, y, tx + tabW, y + SUBTAB_H,
                    sel ? COL_SELECTED_BG : (hov ? COL_HOVER_BG : COL_PANEL));
            ctx.fill(tx, y + SUBTAB_H - (sel ? 2 : 1), tx + tabW, y + SUBTAB_H,
                    sel ? COL_ACCENT : COL_BORDER);
            String label = COSMETIC_ICONS[i] + "  " + COSMETIC_TABS[i];
            int lw = textRenderer.getWidth(label);
            ctx.drawTextWithShadow(textRenderer, label,
                    tx + (tabW - lw) / 2, y + 10,
                    sel ? COL_TEXT : (hov ? 0xFFCCCCFF : COL_TEXT_LABEL));
        }
        y += SUBTAB_H + 12;

        // Katalog laden?
        CosmeticsCatalog catalog = CosmeticsCatalogClient.get();
        if (catalog == null) {
            drawInfoBox(ctx,
                    CosmeticsCatalogClient.isLoading()
                            ? "§7Katalog wird geladen..."
                            : "§cKatalog konnte nicht geladen werden.",
                    x, y, w);
            return;
        }

        // Sicherheits-Fallback: mc.player null (Hauptmenü) → sofort ready
        if (!playerDataReady) {
            MinecraftClient mcFb = MinecraftClient.getInstance();
            if (mcFb.player == null || CosmeticsApiClient.isCached(mcFb.player.getUuid())) {
                playerDataReady = true;
            } else {
                drawInfoBox(ctx, "§7Spieler-Daten werden geladen...", x, y, w);
                return;
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        // playerData kann null sein wenn der Spieler nicht auf dem Server registriert ist
        // → alle Items werden als gesperrt angezeigt (owned_item_ids leer)
        CosmeticsApiResponse playerData = mc.player != null
                ? CosmeticsApiClient.getCosmetics(mc.player.getUuid()) : null;

        // Badges-Tab separat behandeln
        if (selectedCosmeticTab == 4) {
            drawBadgesInCosmetics(ctx, mx, my, x, y, w);
            return;
        }

        String typeName = COSMETIC_TYPE_NAMES[selectedCosmeticTab];
        List<CosmeticsCatalog.CatalogItem> items = catalog.getItemsForType(typeName);

        if (items.isEmpty()) {
            drawInfoBox(ctx, "§7Keine " + COSMETIC_TABS[selectedCosmeticTab] + " verfügbar.", x, y, w);
            return;
        }

        // Karten-Grid
        int cols = Math.max(1, (w + CARD_GAP) / (CARD_W + CARD_GAP));
        for (int i = 0; i < items.size(); i++) {
            CosmeticsCatalog.CatalogItem item = items.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx  = x + col * (CARD_W + CARD_GAP);
            int cy  = y + row * (CARD_H + CARD_GAP);

            boolean owned  = playerData != null && playerData.ownsItem(item.id);
            boolean active = owned && VoxelClientConfig.get().getActiveItemId(typeName) == item.id;
            drawCosmeticCard(ctx, mx, my, cx, cy, item, typeName, owned, active);
        }
    }

    private void drawCosmeticCard(DrawContext ctx, int mx, int my,
                                  int x, int y,
                                  CosmeticsCatalog.CatalogItem item,
                                  String typeName,
                                  boolean owned, boolean active) {
        boolean hov = owned && mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;

        int bg = !owned ? COL_LOCKED_BG
                : active ? COL_ACTIVE_GLOW
                : hov    ? COL_HOVER_BG
                :          COL_PANEL;
        ctx.fill(x, y, x + CARD_W, y + CARD_H, bg);

        int border = active ? COL_ACCENT : COL_BORDER;
        ctx.fill(x,              y,              x + CARD_W, y + 1,          border);
        ctx.fill(x,              y + CARD_H - 1, x + CARD_W, y + CARD_H,     border);
        ctx.fill(x,              y,              x + 1,      y + CARD_H,     border);
        ctx.fill(x + CARD_W - 1, y,              x + CARD_W, y + CARD_H,     border);

        int previewX = x + (CARD_W - CARD_PREVIEW) / 2;
        int previewY = y + 8;

        if (!owned) {
            ctx.fill(previewX, previewY, previewX + CARD_PREVIEW, previewY + CARD_PREVIEW, 0xFF080814);
            ctx.drawCenteredTextWithShadow(textRenderer, "🔒",
                    previewX + CARD_PREVIEW / 2, previewY + CARD_PREVIEW / 2 - 4, COL_LOCKED_TEXT);
        } else if (item.url != null && !item.url.isBlank()) {
            Identifier tex = WebTextureLoader.getOrLoad(item.url, "cositem_" + item.id);
            ctx.fill(previewX, previewY, previewX + CARD_PREVIEW, previewY + CARD_PREVIEW, 0xFF111122);
            if (tex != null) {
                ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                        tex, previewX, previewY, 0, 0,
                        CARD_PREVIEW, CARD_PREVIEW, CARD_PREVIEW, CARD_PREVIEW);
            } else {
                ctx.drawCenteredTextWithShadow(textRenderer, "§7...",
                        previewX + CARD_PREVIEW / 2, previewY + CARD_PREVIEW / 2 - 4, COL_TEXT_DIM);
            }
        } else {
            int iconBg = switch (typeName) {
                case "cape"  -> 0xFF180F28;
                case "halo"  -> 0xFF1A1708;
                case "wings" -> 0xFF0D1A0D;
                case "trail" -> 0xFF1A0D0D;
                default      -> 0xFF111122;
            };
            ctx.fill(previewX, previewY, previewX + CARD_PREVIEW, previewY + CARD_PREVIEW, iconBg);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    COSMETIC_ICONS[selectedCosmeticTab],
                    previewX + CARD_PREVIEW / 2, previewY + CARD_PREVIEW / 2 - 4,
                    active ? 0xFFAABBFF : COL_TEXT_LABEL);
        }

        // Item-Name
        String nameLabel = owned ? (active ? "§b" + item.display : "§f" + item.display) : "§8" + item.display;
        int nlw = textRenderer.getWidth(nameLabel);
        ctx.drawTextWithShadow(textRenderer, nameLabel,
                x + (CARD_W - nlw) / 2, previewY + CARD_PREVIEW + 6,
                owned ? COL_TEXT : COL_LOCKED_TEXT);

        // Status
        String status = !owned ? "§8Gesperrt" : active ? "§b● Aktiv" : "§7○ Inaktiv";
        int slw = textRenderer.getWidth(status);
        ctx.drawTextWithShadow(textRenderer, status,
                x + (CARD_W - slw) / 2, previewY + CARD_PREVIEW + 18, 0xFFFFFFFF);
    }

    // ── HUD ───────────────────────────────────────────────────────────────────

    private void drawHud(DrawContext ctx, int mx, int my, int x, int y, int w) {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        drawSectionHeader(ctx, "HUD Elemente", x, y); y += 22;
        y = drawToggleRow(ctx, mx, my, x, y, w, 0, "FPS-Counter",   "Frames per second",          cfg.hudShowFps,        true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 1, "Koordinaten",   "XYZ-Position im HUD",        cfg.hudShowCoords,     true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 2, "Rüstung",       "Rüstungszustand",            cfg.hudShowArmor,      true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 3, "Blickrichtung", "Himmelsrichtung",             cfg.hudShowDirection,  true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 4, "Speedometer",   "Bewegungsgeschwindigkeit",    cfg.hudShowSpeed,      true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 5, "CPS",           "Klicks pro Sekunde",          cfg.hudShowCps,        true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 6, "Keystrokes",    "Tastenanzeige (WASD, LMB…)", cfg.hudShowKeystrokes, true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 7, "Ping",          "Latenz zum Server (ms)",      cfg.hudShowPing,       true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 8, "Textschatten",  "Schatten unter HUD-Text",     cfg.hudShadow,         true);

        y += 12;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 28;
        ctx.fill(x, y, x + w, y + 28, hov ? 0xFF1E2E5A : 0xFF141828);
        ctx.fill(x, y, x + 2, y + 28, COL_ACCENT);
        ctx.drawTextWithShadow(textRenderer, "§b⊞  HUD-Editor öffnen", x + 10, y + 6, COL_TEXT);
        ctx.drawTextWithShadow(textRenderer, "§7Elemente per Drag & Drop frei positionieren", x + 10, y + 17, COL_TEXT_DIM);
    }

    // ── Visual ────────────────────────────────────────────────────────────────

    private void drawVisual(DrawContext ctx, int mx, int my, int x, int y, int w) {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        drawSectionHeader(ctx, "Zoom", x, y); y += 22;
        y = drawToggleRow(ctx, mx, my, x, y, w, 0, "Smooth Zoom", "Sanftes Easing beim Zoomen", cfg.zoomSmoothZoom, true);
        y += 8;
        drawSectionHeader(ctx, "Freelook", x, y); y += 22;
        drawToggleRow(ctx, mx, my, x, y, w, 1, "Freelook", "Kamera unabhängig drehen", cfg.freelookEnabled, true);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void drawUtility(DrawContext ctx, int mx, int my, int x, int y, int w) {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        drawSectionHeader(ctx, "Allgemein", x, y); y += 22;
        y = drawToggleRow(ctx, mx, my, x, y, w, 0, "Todes-Waypoint",   "Waypoint beim Tod setzen", cfg.deathWaypoint,  true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 1, "Chat-Zeitstempel", "Uhrzeit im Chat anzeigen", cfg.chatTimestamps, true);
        drawToggleRow(ctx, mx, my, x, y, w, 2, "UI-Animationen", "Animationen im Menü",       cfg.uiAnimations,   true);
    }

    // ── Badges Sub-Tab ────────────────────────────────────────────────────────

    private static final int BADGE_CARD_W   = 180;
    private static final int BADGE_CARD_H   = 64;
    private static final int BADGE_CARD_GAP = 8;

    /**
     * Zeichnet den Badges-Tab im Cosmetics-Panel.
     * Zeigt alle verfügbaren Badges; eigene werden hervorgehoben.
     * Das aktive Badge (wird angezeigt) ist togglebar.
     */
    private void drawBadgesInCosmetics(DrawContext ctx, int mx, int my, int x, int y, int w) {
        if (badgesLoading || ownedBadgesLoading) {
            drawInfoBox(ctx, "§7Badges werden geladen...", x, y, w);
            return;
        }
        if (allBadges.isEmpty()) {
            drawInfoBox(ctx, "§7Keine Badges verfügbar.", x, y, w);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        // Aktives Badge aus Config
        int activeBadgeId = VoxelClientConfig.get().activeBadgeId;

        int cols = Math.max(1, (w + BADGE_CARD_GAP) / (BADGE_CARD_W + BADGE_CARD_GAP));
        for (int i = 0; i < allBadges.size(); i++) {
            BadgeInfo badge = allBadges.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx  = x + col * (BADGE_CARD_W + BADGE_CARD_GAP);
            int cy  = y + row * (BADGE_CARD_H + BADGE_CARD_GAP);

            boolean owned  = ownedBadgeIds.contains(badge.id);
            boolean active = owned && activeBadgeId == badge.id;
            drawBadgeCard(ctx, mx, my, cx, cy, badge, owned, active);
        }
    }

    private void handleBadgeTabClick(int mx, int my, int x, int y, int w) {
        if (allBadges.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        CosmeticsApiResponse playerData = mc.player != null
                ? CosmeticsApiClient.getCosmetics(mc.player.getUuid()) : null;
        if (playerData == null) return;

        int cols = Math.max(1, (w + BADGE_CARD_GAP) / (BADGE_CARD_W + BADGE_CARD_GAP));
        for (int i = 0; i < allBadges.size(); i++) {
            BadgeInfo badge = allBadges.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx  = x + col * (BADGE_CARD_W + BADGE_CARD_GAP);
            int cy  = y + row * (BADGE_CARD_H + BADGE_CARD_GAP);

            if (mx >= cx && mx < cx + BADGE_CARD_W && my >= cy && my < cy + BADGE_CARD_H) {
                if (!ownedBadgeIds.contains(badge.id)) return; // gesperrt

                VoxelClientConfig cfg = VoxelClientConfig.get();
                // Toggle: aktiv → deaktivieren; inaktiv → aktivieren
                if (cfg.activeBadgeId == badge.id) {
                    cfg.activeBadgeId = 0;
                } else {
                    cfg.activeBadgeId = badge.id;
                }
                VoxelClientConfig.save();
                return;
            }
        }
    }

    private void drawBadgeCard(DrawContext ctx, int mx, int my,
                               int x, int y, BadgeInfo badge,
                               boolean owned, boolean active) {
        boolean hov = owned && mx >= x && mx < x + BADGE_CARD_W && my >= y && my < y + BADGE_CARD_H;

        int bg = !owned ? COL_LOCKED_BG
                : active ? COL_ACTIVE_GLOW
                : hov    ? COL_HOVER_BG
                :          COL_PANEL;
        ctx.fill(x, y, x + BADGE_CARD_W, y + BADGE_CARD_H, bg);

        // Farbiger linker Streifen in Badge-Farbe
        int badgeArgb = parseBadgeColor(badge.color);
        ctx.fill(x, y, x + 3, y + BADGE_CARD_H, badgeArgb);

        // Rahmen
        int border = active ? badgeArgb : (owned ? COL_BORDER : 0xFF0A0A16);
        ctx.fill(x,                    y,                    x + BADGE_CARD_W, y + 1,              border);
        ctx.fill(x,                    y + BADGE_CARD_H - 1, x + BADGE_CARD_W, y + BADGE_CARD_H,   border);
        ctx.fill(x + BADGE_CARD_W - 1, y,                    x + BADGE_CARD_W, y + BADGE_CARD_H,   border);

        if (!owned) {
            // Gesperrt
            ctx.drawCenteredTextWithShadow(textRenderer, "§8🔒  " + badge.display,
                    x + BADGE_CARD_W / 2, y + BADGE_CARD_H / 2 - 4, COL_LOCKED_TEXT);
        } else {
            // Icon-Box
            ctx.fill(x + 8, y + 14, x + 36, y + 40, (badgeArgb & 0x00FFFFFF) | 0x33000000);
            ctx.drawCenteredTextWithShadow(textRenderer, badge.icon, x + 22, y + 21, badgeArgb);

            // Name
            String nameStr = active ? "§f§l" + badge.display : "§f" + badge.display;
            ctx.drawTextWithShadow(textRenderer, nameStr, x + 44, y + 14, COL_TEXT);

            // Status
            if (active) {
                ctx.drawTextWithShadow(textRenderer, "§b● Wird angezeigt", x + 44, y + 26, 0xFF4A6CF7);
            } else {
                ctx.drawTextWithShadow(textRenderer, "§7○ Klicken zum Aktivieren", x + 44, y + 26, COL_TEXT_DIM);
            }

            // Hinweis: nur eines aktiv
            if (active) {
                ctx.drawTextWithShadow(textRenderer, "§7(Nochmal klicken → ausblenden)",
                        x + 44, y + 38, COL_TEXT_DIM);
            }
        }
    }

    private static int parseBadgeColor(String hex) {
        if (hex == null) return 0xFF888888;
        try { return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16); }
        catch (Exception e) { return 0xFF888888; }
    }

    // ── UI-Komponenten ────────────────────────────────────────────────────────

    private void drawSectionHeader(DrawContext ctx, String title, int x, int y) {
        ctx.drawTextWithShadow(textRenderer, "§b" + title, x, y, COL_TITLE_A);
        ctx.fill(x, y + 11, x + 200, y + 12, COL_BORDER);
    }

    private void drawInfoBox(DrawContext ctx, String text, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 24, COL_SELECTED_BG);
        ctx.fill(x, y, x + 2, y + 24, COL_ACCENT);
        ctx.drawTextWithShadow(textRenderer, text, x + 8, y + 8, COL_TEXT_LABEL);
    }

    private int drawToggleRow(DrawContext ctx, int mx, int my,
                              int x, int y, int w, int rowIdx,
                              String label, String desc, boolean enabled, boolean unlocked) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        ctx.fill(x, y, x + w, y + ROW_H, hov ? COL_HOVER_BG : COL_PANEL);
        ctx.fill(x, y + ROW_H - 1, x + w, y + ROW_H, COL_BORDER);
        ctx.drawTextWithShadow(textRenderer, label, x + 8, y + 6, unlocked ? COL_TEXT : COL_TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, "§7" + desc, x + 8, y + 18, COL_TEXT_DIM);

        if (!unlocked) {
            ctx.drawTextWithShadow(textRenderer, "§7🔒 Nicht verfügbar", x + w - 110, y + 13, COL_TEXT_DIM);
        } else {
            int tx = x + w - TOGGLE_W - 8, ty = y + (ROW_H - TOGGLE_H) / 2;
            ctx.fill(tx, ty, tx + TOGGLE_W, ty + TOGGLE_H, enabled ? COL_ON : COL_OFF);
            int kx = enabled ? tx + TOGGLE_W - TOGGLE_H + 2 : tx + 2;
            ctx.fill(kx, ty + 2, kx + TOGGLE_H - 4, ty + TOGGLE_H - 2, 0xFFFFFFFF);
        }
        return y + ROW_H;
    }

    // ── Mausklicks ────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;

        int cx2 = width - 24;
        if (mx >= cx2 && mx <= cx2 + 18 && my >= 15 && my <= 33) {
            this.close(); return true;
        }

        int catStartY = 18 + 42 + 50;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int cy = catStartY + i * (CAT_H + 2);
            if (mx >= 4 && mx < SIDEBAR_W - 4 && my >= cy && my < cy + CAT_H) {
                selectedCategory = i; return true;
            }
        }

        if (mx >= SIDEBAR_W) {
            int x = SIDEBAR_W + PADDING;
            int w = width - SIDEBAR_W - PADDING * 2;
            VoxelClientConfig cfg = VoxelClientConfig.get();
            MinecraftClient mc = MinecraftClient.getInstance();

            switch (selectedCategory) {

                case 0 -> { // Cosmetics
                    // Sicherstellen dass Tab-Index gültig ist
                    if (selectedCosmeticTab < 0 || selectedCosmeticTab >= COSMETIC_TABS.length) selectedCosmeticTab = 0;
                    int baseY = HEADER_H + PADDING;

                    // Sub-Tab-Klick
                    int tabW = (w - (COSMETIC_TABS.length - 1) * SUBTAB_GAP) / COSMETIC_TABS.length;
                    if (my >= baseY && my < baseY + SUBTAB_H) {
                        for (int i = 0; i < COSMETIC_TABS.length; i++) {
                            int tx = x + i * (tabW + SUBTAB_GAP);
                            if (mx >= tx && mx < tx + tabW) {
                                selectedCosmeticTab = i; return true;
                            }
                        }
                    }

                    // Karten-Klick
                    if (selectedCosmeticTab == 4) {
                        handleBadgeTabClick(mx, my, x, baseY + SUBTAB_H + 12, w);
                        break;
                    }
                    CosmeticsCatalog catalog = CosmeticsCatalogClient.get();
                    if (catalog == null) break;
                    CosmeticsApiResponse playerData = mc.player != null
                            ? CosmeticsApiClient.getCosmetics(mc.player.getUuid()) : null;
                    if (playerData == null) break;

                    String typeName = COSMETIC_TYPE_NAMES[selectedCosmeticTab];
                    List<CosmeticsCatalog.CatalogItem> items = catalog.getItemsForType(typeName);
                    int cardsY = baseY + SUBTAB_H + 12;
                    int cols   = Math.max(1, (w + CARD_GAP) / (CARD_W + CARD_GAP));

                    for (int i = 0; i < items.size(); i++) {
                        int col = i % cols;
                        int row = i / cols;
                        int cx  = x + col * (CARD_W + CARD_GAP);
                        int cy  = cardsY + row * (CARD_H + CARD_GAP);
                        if (mx >= cx && mx < cx + CARD_W && my >= cy && my < cy + CARD_H) {
                            CosmeticsCatalog.CatalogItem item = items.get(i);
                            if (!playerData.ownsItem(item.id)) return true; // gesperrt
                            cfg.toggleItem(typeName, item.id);
                            VoxelClientConfig.save();
                            return true;
                        }
                    }
                }

                case 1 -> { // HUD
                    int baseY = HEADER_H + PADDING + 22;
                    boolean inToggle = mx >= x + w - TOGGLE_W - 20;
                    if (inToggle) {
                        int row = (my - baseY) / ROW_H;
                        switch (row) {
                            case 0 -> { cfg.hudShowFps        = !cfg.hudShowFps;        VoxelClientConfig.save(); }
                            case 1 -> { cfg.hudShowCoords     = !cfg.hudShowCoords;     VoxelClientConfig.save(); }
                            case 2 -> { cfg.hudShowArmor      = !cfg.hudShowArmor;      VoxelClientConfig.save(); }
                            case 3 -> { cfg.hudShowDirection  = !cfg.hudShowDirection;  VoxelClientConfig.save(); }
                            case 4 -> { cfg.hudShowSpeed      = !cfg.hudShowSpeed;      VoxelClientConfig.save(); }
                            case 5 -> { cfg.hudShowCps        = !cfg.hudShowCps;        VoxelClientConfig.save(); }
                            case 6 -> { cfg.hudShowKeystrokes = !cfg.hudShowKeystrokes; VoxelClientConfig.save(); }
                            case 7 -> { cfg.hudShowPing       = !cfg.hudShowPing;       VoxelClientConfig.save(); }
                            case 8 -> { cfg.hudShadow         = !cfg.hudShadow;         VoxelClientConfig.save(); }
                        }
                    }
                    int editorY = HEADER_H + PADDING + 22 + (9 * ROW_H) + 12;
                    if (my >= editorY && my < editorY + 28) {
                        MinecraftClient.getInstance().setScreen(new HudEditorScreen(this));
                        return true;
                    }
                }

                case 2 -> { // Visual
                    boolean inToggle = mx >= x + w - TOGGLE_W - 20;
                    if (!inToggle) break;
                    int baseY = HEADER_H + PADDING + 22;
                    if (my >= baseY && my < baseY + ROW_H) {
                        cfg.zoomSmoothZoom = !cfg.zoomSmoothZoom; VoxelClientConfig.save();
                    }
                    int freelookY = baseY + ROW_H + 8 + 22;
                    if (my >= freelookY && my < freelookY + ROW_H) {
                        cfg.freelookEnabled = !cfg.freelookEnabled; VoxelClientConfig.save();
                    }
                }

                case 3 -> { // Utility
                    boolean inToggle = mx >= x + w - TOGGLE_W - 20;
                    if (!inToggle) break;
                    int baseY = HEADER_H + PADDING + 22;
                    int row   = (my - baseY) / ROW_H;
                    if (row == 0) { cfg.deathWaypoint  = !cfg.deathWaypoint;  VoxelClientConfig.save(); }
                    if (row == 1) { cfg.chatTimestamps = !cfg.chatTimestamps; VoxelClientConfig.save(); }
                    if (row == 2) { cfg.uiAnimations   = !cfg.uiAnimations;   VoxelClientConfig.save(); }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private static String hexToMc(String hex) {
        if (hex == null) return "§7";
        try {
            int c = Integer.parseInt(hex.replace("#", ""), 16);
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            if (r > 200 && g < 100 && b < 100) return "§c";
            if (r > 200 && g > 100 && b < 100) return "§6";
            if (r < 100 && g > 150 && b < 100) return "§a";
            if (r < 100 && g < 100 && b > 200) return "§9";
            if (r > 150 && g < 100 && b > 200) return "§5";
            if (r < 100 && g > 200 && b > 200) return "§b";
            if (r > 150 && g > 150 && b > 150) return "§f";
        } catch (Exception ignored) {}
        return "§7";
    }

    @Override
    public boolean shouldPause() { return false; }
}