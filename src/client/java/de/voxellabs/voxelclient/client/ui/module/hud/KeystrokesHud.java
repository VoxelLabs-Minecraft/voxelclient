package de.voxellabs.voxelclient.client.ui.module.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Keystrokes HUD — VoxelClient v0.0.2
 * Zeigt WASD + LMB/RMB als animierte Tasten-Overlay im HUD an.
 * Position ist über das Drag-and-Drop HUD System verschiebbar.
 */
public class KeystrokesHud {

    // Standard-Position (unten rechts) — wird von DraggableHudSystem überschrieben
    public static int posX = -1; // -1 = auto (bottom-right)
    public static int posY = -1;
    public static boolean enabled = true;

    private static final int KEY_SIZE = 18;
    private static final int KEY_GAP = 2;
    private static final int PRESSED_COLOR = 0xFF5A9E6F;   // Grün wenn gedrückt
    private static final int RELEASED_COLOR = 0x88000000;   // Dunkel wenn losgelassen
    private static final int BORDER_COLOR = 0xFF333333;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public static void register() {
        HudRenderCallback.EVENT.register(KeystrokesHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options == null) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int totalW = KEY_SIZE * 3 + KEY_GAP * 2;       // W A S D = 3 Spalten
        int totalH = KEY_SIZE * 3 + KEY_GAP * 2 + 2;   // 3 Reihen + LMB/RMB

        int x = posX < 0 ? screenW - totalW - 5 : posX;
        int y = posY < 0 ? screenH - totalH - 5 : posY;

        boolean wPressed = client.options.forwardKey.isPressed();
        boolean aPressed = client.options.leftKey.isPressed();
        boolean sPressed = client.options.backKey.isPressed();
        boolean dPressed = client.options.rightKey.isPressed();
        boolean spacePressed = client.options.jumpKey.isPressed();
        boolean lmbPressed = client.options.attackKey.isPressed();
        boolean rmbPressed = client.options.useKey.isPressed();

        // Reihe 1: [W] zentriert
        int wX = x + KEY_SIZE + KEY_GAP;
        drawKey(context, client, wX, y, "W", wPressed);

        // Reihe 2: [A] [S] [D]
        drawKey(context, client, x, y + KEY_SIZE + KEY_GAP, "A", aPressed);
        drawKey(context, client, x + KEY_SIZE + KEY_GAP, y + KEY_SIZE + KEY_GAP, "S", sPressed);
        drawKey(context, client, x + (KEY_SIZE + KEY_GAP) * 2, y + KEY_SIZE + KEY_GAP, "D", dPressed);

        // Reihe 3: [SPACE] (breit)
        drawWideKey(context, client, x, y + (KEY_SIZE + KEY_GAP) * 2, totalW, KEY_SIZE, "SPACE", spacePressed);

        // Reihe 4: [LMB] [RMB]
        int mouseRowY = y + (KEY_SIZE + KEY_GAP) * 3 + 2;
        int halfW = (totalW - KEY_GAP) / 2;
        drawWideKey(context, client, x, mouseRowY, halfW, KEY_SIZE, "LMB", lmbPressed);
        drawWideKey(context, client, x + halfW + KEY_GAP, mouseRowY, halfW, KEY_SIZE, "RMB", rmbPressed);
    }

    private static void drawKey(DrawContext ctx, MinecraftClient client,
                                 int x, int y, String label, boolean pressed) {
        drawWideKey(ctx, client, x, y, KEY_SIZE, KEY_SIZE, label, pressed);
    }

    private static void drawWideKey(DrawContext ctx, MinecraftClient client,
                                     int x, int y, int w, int h, String label, boolean pressed) {
        int bg = pressed ? PRESSED_COLOR : RELEASED_COLOR;

        // Hintergrund
        ctx.fill(x, y, x + w, y + h, bg);
        // Rahmen
        ctx.drawBorder(x, y, w, h, BORDER_COLOR);

        // Text zentriert
        int textW = client.textRenderer.getWidth(label);
        int textX = x + (w - textW) / 2;
        int textY = y + (h - client.textRenderer.fontHeight) / 2 + 1;
        ctx.drawText(client.textRenderer, label, textX, textY, TEXT_COLOR, false);
    }
}
