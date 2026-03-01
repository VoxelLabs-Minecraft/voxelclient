package de.voxellabs.voxelclient.client.gui;

import de.voxellabs.voxelclient.client.discord.DiscordRPCManager;
import de.voxellabs.voxelclient.client.version.VersionChecker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Custom Main Menu Screen.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  [Update-Banner – nur sichtbar wenn Update verfügbar]   │
 *   │                                                         │
 *   │              [MyClient animiertes Logo]                 │
 *   │              Minecraft 1.21.x  |  Fabric                │
 *   │               ─────────────────────────                 │
 *   │                   [ Singleplayer ]                      │
 *   │                   [  Multiplayer ]                      │
 *   │                   [    Realms   ]                       │
 *   │          [ Options ]        [ Quit Game ]               │
 *   │                                                         │
 *   │  MyClient v1.0.0          Plantaria.net ♥ ave.rip       │
 *   └─────────────────────────────────────────────────────────┘
 */
public class CustomMainMenuScreen extends Screen {

    // ── Konstanten ────────────────────────────────────────────────────────────
    private static final String BRANDING_LEFT  = "VoxelClient v" + VersionChecker.CURRENT_VERSION;
    private static final String BRANDING_RIGHT = "Powered by Plantaria.net & ave.rip";

    // ── CustomIcon ───────────────────────────────────────────────────────────
    private static final Identifier LOGO_TEXTURE = Identifier.of("voxelclient", "icons/icon.png");
    private static final int LOGO_SIZE = 64;

    // ── Animations-State ──────────────────────────────────────────────────────
    private static final int BTN_W    = 200;
    private static final int BTN_H    = 20;
    private static final int BANNER_H = 22;

    private int logoY;

    private float animTick  = 0f;
    private float logoBob   = 0f;
    private boolean bobUp   = true;

    private float glowPulse     = 0f;
    private boolean glowPulseUp = true;

    private float bannerAlpha     = 0f;
    private float bannerPulse     = 0f;
    private boolean bannerPulseUp = true;

    // ── Constructor ───────────────────────────────────────────────────────────
    public CustomMainMenuScreen() {
        super(Text.literal("VoxelClient v" + VersionChecker.CURRENT_VERSION));
    }

    // ── Background ─────────────────────────────────────────────────────────────
    // Override to prevent vanilla panorama/darkening from being drawn on top
    // of our custom content when super.render() is called.
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // intentionally empty – we render our own animated background in render()
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        DiscordRPCManager.showMainMenu();
        int cx     = this.width / 2;

        int bannerOffset = VersionChecker.isUpdateAvailable() ? BANNER_H : 0;
        logoY = bannerOffset + (this.height - bannerOffset) / 2 - 115;

        int startY = logoY + LOGO_SIZE + 32;

        // ── Haupt-Buttons ─────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("✦  Singleplayer"),
                        btn -> this.client.setScreen(
                                new net.minecraft.client.gui.screen.world.SelectWorldScreen(this)))
                .dimensions(cx - BTN_W / 2, startY, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("✦  Multiplayer"),
                        btn -> this.client.setScreen(new CustomServerListScreen(this)))
                .dimensions(cx - BTN_W / 2, startY + 24, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("✦  Minecraft Realms"),
                        btn -> this.client.setScreen(
                                new net.minecraft.client.realms.gui.screen.RealmsMainScreen(this)))
                .dimensions(cx - BTN_W / 2, startY + 48, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Options"),
                        btn -> this.client.setScreen(
                                new OptionsScreen(this, this.client.options)))
                .dimensions(cx - BTN_W / 2, startY + 76, (BTN_W / 2) - 2, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Quit Game"),
                        btn -> this.client.scheduleStop())
                .dimensions(cx + 2, startY + 76, (BTN_W / 2) - 2, BTN_H).build());

        // ── Settings-Button (oben rechts) ─────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("⚙"),
                        btn -> this.client.setScreen(new ClientModScreen(this)))
                .dimensions(this.width - 24, 4, 20, 20).build());

        // ── Update-Banner-Button (nur wenn Update verfügbar) ─────────────────
        if (VersionChecker.isUpdateAvailable()) {
            String label = "§e⬆ Update auf v" + VersionChecker.getLatestVersion()
                    + " verfügbar – Hier klicken zum Herunterladen";

            addDrawableChild(ButtonWidget.builder(
                            Text.literal(label),
                            btn -> openDownloadPage())
                    .dimensions(0, 0, this.width, BANNER_H).build());
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Animations-Tick
        animTick += delta * 0.5f;

        if (bobUp) { logoBob += delta * 0.012f; if (logoBob >= 1f)   { logoBob = 1f;   bobUp = false; } }
        else       { logoBob -= delta * 0.012f; if (logoBob <= -1f)  { logoBob = -1f;  bobUp = true;  } }

        if (glowPulseUp) { glowPulse += delta * 0.022f; if (glowPulse >= 1f)   { glowPulse = 1f;   glowPulseUp = false; } }
        else             { glowPulse -= delta * 0.022f; if (glowPulse <= 0.3f) { glowPulse = 0.3f; glowPulseUp = true;  } }

        // ── Hintergrund ───────────────────────────────────────────────────────
        renderAnimatedBackground(ctx);

        // ── Update-Banner ─────────────────────────────────────────────────────
        if (VersionChecker.isUpdateAvailable()) {
            renderUpdateBanner(ctx, mouseX, mouseY, delta);
        } else if (VersionChecker.isChecking()) {
            // Kleiner Hinweis während des Checks
            ctx.drawTextWithShadow(this.textRenderer,
                    "§8Prüfe auf Updates…",
                    4, this.height - 20, 0x444444);
        }

        // ── Logo ──────────────────────────────────────────────────────────────
        int cx = this.width / 2;
        int bob = (int)(logoBob * 2.5f);

        renderLogoGlow(ctx, cx, logoY + bob);

        int logoX = cx - LOGO_SIZE / 2;
//        ctx.drawTexture(
//                net.minecraft.client.render.RenderLayer::getGuiTextured,
//                LOGO_TEXTURE,
//                logoX, logoY + bob,
//                0, 0,
//                LOGO_SIZE, LOGO_SIZE,
//                LOGO_SIZE, LOGO_SIZE
//        );

        int r = (int)(60  + 80  * MathHelper.sin(animTick * 0.04f));
        int g = (int)(100 + 100 * MathHelper.sin(animTick * 0.03f + 1f));
        int b = (int)(200 + 55  * MathHelper.sin(animTick * 0.05f + 2f));
        int titleColor = 0xFF000000 | (r << 16) | (g << 8) | b;

        ctx.drawCenteredTextWithShadow(this.textRenderer, "VoxelClient",
                cx + 1, this.height / 2 - 90 + 1, 0x222222);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "VoxelClient",
                cx,     this.height / 2 - 90,     titleColor);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Minecraft 1.21.x  §8|  §7Fabric Edition"),
                cx, this.height / 2 - 74, 0xAAAAAA);

        // Trennlinie
        int divY = this.height / 2 - 62;
        ctx.fill(cx - 120, divY, cx + 120, divY + 1, 0x66FFFFFF);

        // ── Branding Footer ───────────────────────────────────────────────────
        ctx.drawTextWithShadow(this.textRenderer, BRANDING_LEFT,
                4, this.height - 14, 0xAAAAAA);
        ctx.drawTextWithShadow(this.textRenderer, BRANDING_RIGHT,
                this.width - this.textRenderer.getWidth(BRANDING_RIGHT) - 4,
                this.height - 14, 0xAAAAAA);

        // Buttons rendern (über allem)
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderLogoGlow(DrawContext ctx, int cx, int topY) {
        int cy = topY + LOGO_SIZE / 2;

        int r = (int)(40  + 40  * MathHelper.sin(animTick * 0.03f));
        int g = (int)(80  + 60  * MathHelper.sin(animTick * 0.025f + 1f));
        int b = (int)(180 + 60  * MathHelper.sin(animTick * 0.04f  + 2f));

        int[] radii  = { 52, 44, 38, 32 };
        int[] alphas = { (int)(15 * glowPulse), (int)(25 * glowPulse),
                (int)(35 * glowPulse), (int)(20 * glowPulse) };

        for (int i = 0; i < radii.length; i++) {
            int rad = radii[i];
            int col = (alphas[i] << 24) | (r << 16) | (g << 8) | b;
            ctx.fill(cx - rad, cy - rad, cx + rad, cy + rad, col);
        }
    }

    private void renderUpdateBanner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Fade-In
        if (bannerAlpha < 1f) bannerAlpha = Math.min(1f, bannerAlpha + delta * 0.04f);

        // Pulsierender Rand
        if (bannerPulseUp) { bannerPulse += delta * 0.025f; if (bannerPulse >= 1f)   { bannerPulse = 1f;   bannerPulseUp = false; } }
        else               { bannerPulse -= delta * 0.025f; if (bannerPulse <= 0.3f) { bannerPulse = 0.3f; bannerPulseUp = true;  } }

        int w = this.width;
        int alpha = (int)(bannerAlpha * 255);

        // Hintergrund des Banners (dunkel-goldenes Gradient)
        int bgTop    = (alpha << 24) | 0x1A1200;
        int bgBottom = (alpha << 24) | 0x2A1E00;
        ctx.fillGradient(0, 0, w, BANNER_H, bgTop, bgBottom);

        // Leuchtender oberer Rand (gold/gelb, pulsierend)
        int borderAlpha = (int)(bannerAlpha * bannerPulse * 255);
        int borderColor = (borderAlpha << 24) | 0xFFCC00;
        ctx.fill(0, 0, w, 1, borderColor);

        // Leuchtender unterer Rand
        int borderAlpha2 = (int)(bannerAlpha * 0.6f * 255);
        ctx.fill(0, BANNER_H - 1, w, BANNER_H, (borderAlpha2 << 24) | 0xAA8800);

        // Linker farbiger Akzentbalken
        ctx.fill(0, 0, 3, BANNER_H, (alpha << 24) | 0xFFCC00);

        // Hover-Highlight
        boolean hovered = mouseY >= 0 && mouseY < BANNER_H;
        if (hovered) {
            ctx.fill(0, 0, w, BANNER_H, 0x22FFDD00);
        }

        // ─── Text ─────────────────────────────────────────────────────────────
        int cx     = w / 2;
        int textA  = (int)(bannerAlpha * 255);

        // Icon + Haupttext
        String mainText = "§e⬆  MyClient §fv" + VersionChecker.getLatestVersion()
                + " §7ist verfügbar!  §8(aktuell: v" + VersionChecker.CURRENT_VERSION + ")";
        int mainColor   = (textA << 24) | 0xFFFFFF;

        // Zentriert, 4px von oben im Banner
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(mainText),
                cx, 4, mainColor);

        // Klick-Hinweis (rechte Seite)
        String hint = hovered ? "§e→ Klicken zum Öffnen" : "§8→ Klicken zum Öffnen";
        ctx.drawTextWithShadow(this.textRenderer, hint,
                w - this.textRenderer.getWidth(hint) - 8, 7, 0xFFFFFF);
    }

    // ── Tastatureingabe ───────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // [U] → Download-Seite öffnen (wenn Update verfügbar)
        if (keyCode == 85 && VersionChecker.isUpdateAvailable()) { // 85 = U
            openDownloadPage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────
    private void openDownloadPage() {
        try {
            java.awt.Desktop.getDesktop().browse(
                    java.net.URI.create(VersionChecker.DOWNLOAD_URL));
        } catch (Exception e) {
            System.err.println("[MyClient] Konnte Browser nicht öffnen: " + e.getMessage());
        }
    }

    private void renderAnimatedBackground(DrawContext ctx) {
        int w = this.width;
        int h = this.height;

        ctx.fillGradient(0, 0, w, h, 0xFF0a0a14, 0xFF060610);

        float t = animTick * 0.015f;

        int blobX1 = (int)(w * 0.25f + w * 0.15f * MathHelper.sin(t));
        int blobY1 = (int)(h * 0.25f + h * 0.10f * MathHelper.cos(t * 1.3f));
        drawRadialGlow(ctx, blobX1, blobY1, 200, 0x22 << 24 | 0x2244AA);

        int blobX2 = (int)(w * 0.75f + w * 0.10f * MathHelper.sin(t * 0.8f + 2f));
        int blobY2 = (int)(h * 0.70f + h * 0.08f * MathHelper.cos(t * 1.1f));
        drawRadialGlow(ctx, blobX2, blobY2, 180, 0x22 << 24 | 0x441166);

        for (int y = 0; y < h; y += 4) {
            ctx.fill(0, y, w, y + 1, 0x08000000);
        }
    }

    private void drawRadialGlow(DrawContext ctx, int x, int y, int radius, int color) {
        int steps = 8;
        for (int i = steps; i >= 1; i--) {
            int r      = radius * i / steps;
            int alpha  = (color >> 24 & 0xFF) * (steps - i + 1) / steps;
            int col    = (alpha << 24) | (color & 0x00FFFFFF);
            ctx.fill(x - r, y - r / 2, x + r, y + r / 2, col);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
