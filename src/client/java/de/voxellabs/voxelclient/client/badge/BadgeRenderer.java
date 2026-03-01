package de.voxellabs.voxelclient.client.badge;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import java.util.UUID;

/**
 * Rendert ein kleines "V"-Logo-Badge vor Spielernamen.
 *
 * Farben:
 *   Creatoren  → 0xFFAA0000  (dunkelrot)
 *   Alle anderen → 0xFFAAAAAA  (grau)
 *
 * Das Badge ist ein einfaches farbiges "✦" Zeichen mit
 * einem dunklen Hintergrund-Rechteck dahinter.
 *
 * Wird genutzt von:
 *   - PlayerListHudMixin   → Tab-Liste
 *   - PlayerNametagMixin   → Namensschild über dem Kopf
 */
public final class BadgeRenderer {

    // ── Badge-Zeichen ─────────────────────────────────────────────────────────
    /** Das Zeichen das als Badge angezeigt wird */
    private static final String BADGE_CHAR = "✦";

    // ── Farben ────────────────────────────────────────────────────────────────
    /** Farbe für Creatoren: Dunkelrot */
    public static final int COLOR_CREATOR  = 0xFFCC2200;
    /** Farbe für normale Spieler: Grau */
    public static final int COLOR_PLAYER   = 0xFF888888;
    /** Hintergrund-Farbe des Badges */
    private static final int COLOR_BG      = 0x55000000;

    /** Breite des Badges inkl. Abstand zum Namen */
    public static final int BADGE_WIDTH = 10;

    private BadgeRenderer() {}

    // ── Öffentliche API ───────────────────────────────────────────────────────

    /**
     * Rendert das Badge an Position (x, y).
     * Muss VOR dem Spielernamen aufgerufen werden.
     *
     * @param ctx          DrawContext
     * @param textRenderer TextRenderer der aktuellen GUI
     * @param x            X-Position (linke Kante des Badges)
     * @param y            Y-Position (obere Kante)
     * @param uuid         UUID des Spielers → bestimmt die Farbe
     */
    public static void render(DrawContext ctx, TextRenderer textRenderer,
                              int x, int y, UUID uuid) {
        boolean isCreator = CreatorList.isCreator(uuid);
        int color = isCreator ? COLOR_CREATOR : COLOR_PLAYER;

        // Hintergrund
        ctx.fill(x - 1, y - 1, x + BADGE_WIDTH - 1, y + 8, COLOR_BG);

        // Badge-Zeichen
        ctx.drawTextWithShadow(textRenderer, BADGE_CHAR, x, y, color);
    }

    /**
     * Gibt die Farbe für einen Spieler zurück.
     * Nützlich wenn man nur die Farbe ohne das Rendering braucht.
     *
     * @param uuid UUID des Spielers
     * @return ARGB-Farbe
     */
    public static int getColor(UUID uuid) {
        return CreatorList.isCreator(uuid) ? COLOR_CREATOR : COLOR_PLAYER;
    }

    /**
     * Gibt das Badge als formatierten String zurück.
     * Nützlich für Text-basiertes Rendering (z.B. Nametags).
     *
     * Format: §<farbe>✦§r
     *
     * @param uuid UUID des Spielers
     * @return Formatierter String mit Badge-Zeichen
     */
    public static String getBadgeString(UUID uuid) {
        boolean isCreator = CreatorList.isCreator(uuid);
        // §4 = dunkelrot, §7 = grau
        return isCreator ? "§4✦ §r" : "§7✦ §r";
    }
}
