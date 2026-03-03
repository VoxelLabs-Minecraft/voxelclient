package de.voxellabs.voxelclient.client.ui.module.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Direction;

public class DirectionHud {

    public static boolean enabled = true;
    public static int posX = -1;
    public static int posY = -1;

    public static void register() {
        HudRenderCallback.EVENT.register(DirectionHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Direction direction = client.player.getHorizontalFacing();
        String dirName = switch (direction) {
            case NORTH -> "North (-Z)";
            case SOUTH -> "South (+Z)";
            case EAST  -> "East  (+X)";
            case WEST  -> "West  (-X)";
            default    -> direction.getName();
        };

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        String label = "§7Direction: §f" + dirName;
        int textW = client.textRenderer.getWidth(label);

        int x = posX < 0 ? screenW - textW - 5 : posX;
        int y = posY < 0 ? 5 : posY;

        context.fill(x - 2, y - 2, x + textW + 2, y + 10, 0x88000000);
        context.drawText(client.textRenderer, label, x, y, 0xFFFFFFFF, true);
    }
}
