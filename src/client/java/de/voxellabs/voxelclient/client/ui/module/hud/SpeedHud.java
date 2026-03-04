package de.voxellabs.voxelclient.client.ui.module.hud;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class SpeedHud {


    public static boolean enabled = true;
    public static int posX = -1;
    public static int posY = -1;

    public static void register() {
        HudRenderCallback.EVENT.register(SpeedHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled || !VoxelClientConfig.get().hudShowSpeed) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        double dx = client.player.getX() - client.player.prevX;
        double dz = client.player.getZ() - client.player.prevZ;
        double speed = Math.sqrt(dx * dx + dz * dz) * 20;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        String label = String.format("§7Speed:§f %.2f b/s", speed);
        int textW = client.textRenderer.getWidth(label);

        int x = posX < 0 ? screenW - textW - 5 : posX;
        int y = posY < 0 ? 5 : posY;

        context.fill(x - 2, y - 2, x + textW + 2, y + 10, 0x88000000);
        context.drawText(client.textRenderer, label, x, y, 0xFFFFFFFF, true);
    }
}
