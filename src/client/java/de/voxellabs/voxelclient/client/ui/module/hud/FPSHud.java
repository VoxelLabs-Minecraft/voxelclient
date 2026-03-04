package de.voxellabs.voxelclient.client.ui.module.hud;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class FPSHud {

    public static boolean enabled = true;
    public static int posX = -1;
    public static int posY = -1;

    public static void register() {
        HudRenderCallback.EVENT.register(FPSHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled || !VoxelClientConfig.get().hudShowFps) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;


        int fps = client.getCurrentFps();

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        String label = "§7FPS: " +  formatFPS(fps);
        int textW = client.textRenderer.getWidth(label);

        int x = posX < 0 ? screenW - textW : posX;
        int y = posY < 0 ? screenH - textW : posY;

        context.fill(x - 2, y - 2, x + textW + 2, y + 10, 0x88000000);
        context.drawText(client.textRenderer, label, x, y, 0xFFFFFFFF, true);
    }

    private static String formatFPS(int fps) {
        if (fps < 0) return "§7N/A";
        if (fps > 144) return "§a" + fps;       // Grün — ausgezeichnet
        if (fps > 100) return "§2" + fps;       // Dunkelgrün — sehr gut
        if (fps > 60)  return "§e" + fps;       // Gelb — ok
        if (fps > 30)  return "§6" + fps;       // Orange — schlecht
        return "§c" + fps;                      // Rot — sehr schlecht
    }


}
