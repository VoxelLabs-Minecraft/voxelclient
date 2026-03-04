package de.voxellabs.voxelclient.client.ui.module.hud;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * CPS Counter — VoxelClient v0.0.2
 * Zeigt Klicks pro Sekunde (LMB & RMB) im HUD an.
 * Berechnung basiert auf einem 1-Sekunden-Sliding-Window.
 */
public class CpsCounter {

    public static boolean enabled = true;
    public static int posX = -1;
    public static int posY = -1;

    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();

    private static boolean lastLmb = false;
    private static boolean lastRmb = false;

    public static void register() {
        // Tick-basiertes Tracking
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean lmb = client.options.attackKey.isPressed();
            boolean rmb = client.options.useKey.isPressed();

            long now = System.currentTimeMillis();

            if (lmb && !lastLmb) leftClicks.addLast(now);
            if (rmb && !lastRmb) rightClicks.addLast(now);

            lastLmb = lmb;
            lastRmb = rmb;

            // Ältere Klicks entfernen (> 1 Sekunde)
            pruneOld(leftClicks, now);
            pruneOld(rightClicks, now);
        });

        HudRenderCallback.EVENT.register(CpsCounter::render);
    }

    private static void pruneOld(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) {
            deque.pollFirst();
        }
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled || !VoxelClientConfig.get().hudShowCps) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int lCps = leftClicks.size();
        int rCps = rightClicks.size();

        String text = "§7CPS: §f" + lCps + " §8| §f" + rCps;

        int x = posX < 0 ? screenW - client.textRenderer.getWidth(text) - 5 : posX;
        int y = posY < 0 ? screenH - 30 : posY;

        // Hintergrund
        int textW = client.textRenderer.getWidth(text);
        context.fill(x - 2, y - 2, x + textW + 2, y + 10, 0x88000000);

        context.drawText(client.textRenderer, text, x, y, 0xFFFFFFFF, true);
    }

    public static int getLeftCps() {
        return leftClicks.size();
    }

    public static int getRightCps() {
        return rightClicks.size();
    }
}
