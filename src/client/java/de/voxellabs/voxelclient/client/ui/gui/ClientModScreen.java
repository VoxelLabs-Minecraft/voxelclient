package de.voxellabs.voxelclient.client.ui.gui;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiResponse;
import de.voxellabs.voxelclient.client.ui.hud.HudEditorScreen;
import de.voxellabs.voxelclient.client.version.VersionChecker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
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

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int SIDEBAR_W = 160;
    private static final int PADDING   = 16;
    private static final int CAT_H     = 36;
    private static final int ROW_H     = 34;
    private static final int TOGGLE_W  = 36;
    private static final int TOGGLE_H  = 18;
    private static final int HEADER_H  = 50;

    // ── Kategorien ────────────────────────────────────────────────────────────
    private static final String[] CATEGORIES = { "Cosmetics", "HUD", "Visual", "Utility" };
    private static final String[] CAT_ICONS  = { "✦", "◈", "◉", "⚙" };
    private int selectedCategory = 0;

    // ── Badge ─────────────────────────────────────────────────────────────────
    private String  badgeDisplay = null;
    private String  badgeColor   = null;
    private String  badgeIcon    = null;
    private boolean loadingBadge = true;

    public ClientModScreen() {
        super(Text.literal("VoxelClient"));
    }

    @Override
    protected void init() {
        loadBadge();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) CosmeticsApiClient.prefetch(mc.player.getUuid());
    }

    private void loadBadge() {
        loadingBadge = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { loadingBadge = false; return; }
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
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Kein Minecraft-Blur
    }

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

        // Logo
        int logoY = 18;
        ctx.fill(PADDING, logoY, PADDING + 26, logoY + 26, COL_ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer, "V", PADDING + 13, logoY + 9, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "§bVoxel§5Client", PADDING + 32, logoY + 9, COL_TEXT);
        ctx.fill(PADDING, logoY + 34, SIDEBAR_W - PADDING, logoY + 35, COL_BORDER);

        // Spieler-Info
        int playerY = logoY + 42;
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = mc.player != null ? mc.player.getName().getString() : "?";
        ctx.drawTextWithShadow(textRenderer, "§f" + name, PADDING, playerY, COL_TEXT);

        String badgeStr = loadingBadge ? "§7Lädt..."
                : badgeDisplay != null
                ? hexToMc(badgeColor) + (badgeIcon != null ? badgeIcon : "✦") + " " + badgeDisplay
                : "§7Kein Badge";
        ctx.drawTextWithShadow(textRenderer, badgeStr, PADDING, playerY + 12, COL_TEXT_DIM);
        ctx.fill(PADDING, playerY + 26, SIDEBAR_W - PADDING, playerY + 27, COL_BORDER);

        // Kategorien
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

        // Version
        ctx.drawTextWithShadow(textRenderer, "§7v" + VersionChecker.CURRENT_VERSION,
                PADDING, height - 12, COL_TEXT_DIM);
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

        // ✕ Button
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

    // ── Kategorien ────────────────────────────────────────────────────────────

    private void drawCosmetics(DrawContext ctx, int mx, int my, int x, int y, int w) {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        CosmeticsApiResponse api = mc.player != null
                ? CosmeticsApiClient.getCosmetics(mc.player.getUuid()) : null;

        drawSectionHeader(ctx, "Deine Cosmetics", x, y); y += 22;
        drawInfoBox(ctx, api == null
                        ? "§7Cosmetics werden geladen..."
                        : "§7Server-seitig verwaltet – Toggle aktiviert/deaktiviert die Anzeige.",
                x, y, w);
        y += 30;

        y = drawToggleRow(ctx, mx, my, x, y, w, 0, "Cape",  "Umhang auf dem Rücken",      cfg.cosmeticCapeEnabled,  api != null && api.hasCape());
        y = drawToggleRow(ctx, mx, my, x, y, w, 1, "Halo",  "Ring über dem Kopf",         cfg.cosmeticHaloEnabled,  api != null && api.hasHalo());
        y = drawToggleRow(ctx, mx, my, x, y, w, 2, "Wings", "Flügel auf dem Rücken",      cfg.cosmeticWingsEnabled, api != null && api.hasWings());
        drawToggleRow(ctx, mx, my, x, y, w, 3, "Trail", "Partikel-Spur beim Bewegen", cfg.cosmeticTrailEnabled, api != null && api.hasTrail());
    }

    private void drawHud(DrawContext ctx, int mx, int my, int x, int y, int w) {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        drawSectionHeader(ctx, "HUD Elemente", x, y); y += 22;
        y = drawToggleRow(ctx, mx, my, x, y, w, 0, "FPS-Counter",   "Frames per second",       cfg.hudShowFps,       true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 1, "Koordinaten",   "XYZ-Position im HUD",     cfg.hudShowCoords,    true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 2, "Rüstung",       "Rüstungszustand",         cfg.hudShowArmor,     true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 3, "Blickrichtung", "Himmelsrichtung",          cfg.hudShowDirection, true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 4, "Speedometer",   "Bewegungsgeschwindigkeit", cfg.hudShowSpeed,     true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 5, "Textschatten",  "Schatten unter HUD-Text",  cfg.hudShadow,        true);

        y += 12;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 28;
        ctx.fill(x, y, x + w, y + 28, hov ? 0xFF1E2E5A : 0xFF141828);
        ctx.fill(x, y, x + 2, y + 28, COL_ACCENT);
        ctx.drawTextWithShadow(textRenderer, "§b⊞  HUD-Editor öffnen", x + 10, y + 6, COL_TEXT);
        ctx.drawTextWithShadow(textRenderer, "§7Elemente per Drag & Drop frei positionieren", x + 10, y + 17, COL_TEXT_DIM);
    }

    private void drawVisual(DrawContext ctx, int mx, int my, int x, int y, int w) {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        drawSectionHeader(ctx, "Zoom", x, y); y += 22;
        y = drawToggleRow(ctx, mx, my, x, y, w, 0, "Smooth Zoom", "Sanftes Easing beim Zoomen", cfg.zoomSmoothZoom,  true);
        y += 8;
        drawSectionHeader(ctx, "Freelook", x, y); y += 22;
        drawToggleRow(ctx, mx, my, x, y, w, 1, "Freelook",    "Kamera unabhängig drehen",   cfg.freelookEnabled, true);
    }

    private void drawUtility(DrawContext ctx, int mx, int my, int x, int y, int w) {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        drawSectionHeader(ctx, "Allgemein", x, y); y += 22;
        y = drawToggleRow(ctx, mx, my, x, y, w, 0, "Todes-Waypoint",   "Waypoint beim Tod setzen", cfg.deathWaypoint,  true);
        y = drawToggleRow(ctx, mx, my, x, y, w, 1, "Chat-Zeitstempel", "Uhrzeit im Chat anzeigen", cfg.chatTimestamps, true);
        drawToggleRow(ctx, mx, my, x, y, w, 2, "UI-Animationen",   "Animationen im Menü",      cfg.uiAnimations,   true);
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

        // ✕ schließen
        int cx2 = width - 24;
        if (mx >= cx2 && mx <= cx2 + 18 && my >= 15 && my <= 33) {
            this.close(); return true;
        }

        // Sidebar Kategorien
        int catStartY = 18 + 42 + 50;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int cy = catStartY + i * (CAT_H + 2);
            if (mx >= 4 && mx < SIDEBAR_W - 4 && my >= cy && my < cy + CAT_H) {
                selectedCategory = i; return true;
            }
        }

        // Content-Klicks
        if (mx >= SIDEBAR_W) {
            int x = SIDEBAR_W + PADDING, w = width - SIDEBAR_W - PADDING * 2;
            boolean inToggle = mx >= x + w - TOGGLE_W - 20;
            VoxelClientConfig cfg = VoxelClientConfig.get();
            MinecraftClient mc = MinecraftClient.getInstance();
            CosmeticsApiResponse api = mc.player != null
                    ? CosmeticsApiClient.getCosmetics(mc.player.getUuid()) : null;

            switch (selectedCategory) {
                case 0 -> { // Cosmetics
                    if (!inToggle) break;
                    int baseY = HEADER_H + PADDING + 22 + 30;
                    int row = (my - baseY) / ROW_H;
                    if (row == 0 && api != null && api.hasCape())  { cfg.cosmeticCapeEnabled  = !cfg.cosmeticCapeEnabled;  VoxelClientConfig.save(); }
                    if (row == 1 && api != null && api.hasHalo())  { cfg.cosmeticHaloEnabled  = !cfg.cosmeticHaloEnabled;  VoxelClientConfig.save(); }
                    if (row == 2 && api != null && api.hasWings()) { cfg.cosmeticWingsEnabled = !cfg.cosmeticWingsEnabled; VoxelClientConfig.save(); }
                    if (row == 3 && api != null && api.hasTrail()) { cfg.cosmeticTrailEnabled = !cfg.cosmeticTrailEnabled; VoxelClientConfig.save(); }
                }
                case 1 -> { // HUD
                    int baseY = HEADER_H + PADDING + 22;
                    if (inToggle) {
                        int row = (my - baseY) / ROW_H;
                        switch (row) {
                            case 0 -> { cfg.hudShowFps       = !cfg.hudShowFps;       VoxelClientConfig.save(); }
                            case 1 -> { cfg.hudShowCoords    = !cfg.hudShowCoords;    VoxelClientConfig.save(); }
                            case 2 -> { cfg.hudShowArmor     = !cfg.hudShowArmor;     VoxelClientConfig.save(); }
                            case 3 -> { cfg.hudShowDirection = !cfg.hudShowDirection; VoxelClientConfig.save(); }
                            case 4 -> { cfg.hudShowSpeed     = !cfg.hudShowSpeed;     VoxelClientConfig.save(); }
                            case 5 -> { cfg.hudShadow        = !cfg.hudShadow;        VoxelClientConfig.save(); }
                        }
                    }
                    // HUD-Editor Button
                    int editorY = HEADER_H + PADDING + 22 + (6 * ROW_H) + 12;
                    if (my >= editorY && my < editorY + 28) {
                        MinecraftClient.getInstance().setScreen(new HudEditorScreen(this));
                        return true;
                    }
                }
                case 2 -> { // Visual
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
                    if (!inToggle) break;
                    int baseY = HEADER_H + PADDING + 22;
                    int row = (my - baseY) / ROW_H;
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