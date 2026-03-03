package de.voxellabs.voxelclient.client.ui.animation;

/**
 * UI Animations — VoxelClient v0.0.2
 * Stellt einfache Animationen für Screens bereit:
 * - Fade In (Opacity 0 → 1)
 * - Slide In von unten
 * - Easing-Funktionen
 */
public class UiAnimation {

    public enum Type {
        FADE_IN,
        SLIDE_UP,
        FADE_SLIDE_UP
    }

    private final Type type;
    private final long durationMs;
    private long startTime = -1;
    private boolean finished = false;

    public UiAnimation(Type type, long durationMs) {
        this.type = type;
        this.durationMs = durationMs;
    }

    /** Startet die Animation (idempotent, kann mehrfach aufgerufen werden). */
    public void start() {
        startTime = System.currentTimeMillis();
        finished = false;
    }

    /** Gibt den aktuellen Fortschritt zurück (0.0 → 1.0, mit Easing). */
    public float getProgress() {
        if (startTime < 0) return 1.0f;
        long elapsed = System.currentTimeMillis() - startTime;
        float raw = Math.min(1.0f, (float) elapsed / durationMs);
        return easeOutCubic(raw);
    }

    public boolean isFinished() {
        return getProgress() >= 1.0f;
    }

    /**
     * Gibt den aktuellen Alpha-Wert zurück (0–255).
     */
    public int getAlpha() {
        float p = getProgress();
        return switch (type) {
            case FADE_IN, FADE_SLIDE_UP -> (int)(p * 255);
            case SLIDE_UP -> 255;
        };
    }

    /**
     * Gibt den Y-Offset für Slide-Animationen zurück (positiv = nach unten verschoben).
     */
    public int getYOffset(int maxOffset) {
        float p = getProgress();
        return switch (type) {
            case SLIDE_UP, FADE_SLIDE_UP -> (int)((1.0f - p) * maxOffset);
            case FADE_IN -> 0;
        };
    }

    // ─── EASING ──────────────────────────────────────────────────────────────

    private static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private static float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
    }

    // ─── VORDEFINIERTE ANIMATIONEN ────────────────────────────────────────────

    /** Standard-Fade für alle VoxelClient-Screens (250ms). */
    public static UiAnimation screenFade() {
        return new UiAnimation(Type.FADE_SLIDE_UP, 250);
    }

    /** Schnelle Fade für HUD-Elemente (150ms). */
    public static UiAnimation hudFade() {
        return new UiAnimation(Type.FADE_IN, 150);
    }
}
