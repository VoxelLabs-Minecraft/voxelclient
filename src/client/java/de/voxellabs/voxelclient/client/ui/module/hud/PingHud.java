package de.voxellabs.voxelclient.client.ui.module.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Ping Anzeige — VoxelClient v0.0.2
 * Zeigt den aktuellen Ping (ms) im HUD an, farbcodiert nach Qualität.
 */
public class PingHud {

    public static boolean enabled = true;
    public static int posX = -1;
    public static int posY = -1;

    public static void register() {
        HudRenderCallback.EVENT.register(PingHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;

        // Eigenen Ping abrufen
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        if (entry == null) return;

        int ping = entry.getLatency();
        String pingText = formatPing(ping);

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        String label = "§7Ping: " + pingText;
        int textW = client.textRenderer.getWidth(label);

        int x = posX < 0 ? screenW - textW - 5 : posX;
        int y = posY < 0 ? 5 : posY;

        context.fill(x - 2, y - 2, x + textW + 2, y + 10, 0x88000000);
        context.drawText(client.textRenderer, label, x, y, 0xFFFFFFFF, true);
    }

    private static String formatPing(int ping) {
        if (ping < 0) return "§7N/A";
        if (ping < 50) return "§a" + ping + "ms";        // Grün — ausgezeichnet
        if (ping < 100) return "§2" + ping + "ms";       // Dunkelgrün — gut
        if (ping < 150) return "§e" + ping + "ms";       // Gelb — ok
        if (ping < 300) return "§6" + ping + "ms";       // Orange — schlecht
        return "§c" + ping + "ms";                        // Rot — sehr schlecht
    }

    public static int getPing() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return -1;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : -1;
    }
}
