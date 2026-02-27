package de.voxellabs.voxelclient.client.hud;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders custom HUD elements (FPS, coordinates, armor, direction, speed).
 */
public class HudRenderer {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    /** Call once during client init to register the HUD callback. */
    public static void register() {
        HudRenderCallback.EVENT.register(HudRenderer::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (MC.options.hudHidden || MC.getDebugHud().shouldShowDebugHud()) return;

        ClientPlayerEntity player = MC.player;
        if (player == null) return;

        VoxelClientConfig cfg = VoxelClientConfig.get();
        List<String> lines = getStrings(cfg, player);

        // Render text lines
        int lineHeight = MC.textRenderer.fontHeight + 2;
        int x = cfg.hudX;
        int y = cfg.hudY;
        int color = 0xFF000000 | cfg.hudColor;

        for (int i = 0; i < lines.size(); i++) {
            if (cfg.hudShadow) {
                ctx.drawTextWithShadow(MC.textRenderer, lines.get(i), x, y + i * lineHeight, color);
            } else {
                ctx.drawText(MC.textRenderer, lines.get(i), x, y + i * lineHeight, color, false);
            }
        }

        // Armor durability bar (bottom-left, above hotbar)
        if (cfg.hudShowArmor) {
            renderArmorHud(ctx, player);
        }
    }

    private static @NotNull List<String> getStrings(VoxelClientConfig cfg, ClientPlayerEntity player) {
        List<String> lines  = new ArrayList<>();

        // FPS
        if (cfg.hudShowFps) {
            lines.add("FPS: " + MC.getCurrentFps());
        }

        // Coordinates
        if (cfg.hudShowCoords) {
            BlockPos pos = player.getBlockPos();
            lines.add(String.format("XYZ: %d / %d / %d", pos.getX(), pos.getY(), pos.getZ()));
        }

        // Direction
        if (cfg.hudShowDirection) {
            Direction dir = player.getHorizontalFacing();
            String dirName = switch (dir) {
                case NORTH -> "North (-Z)";
                case SOUTH -> "South (+Z)";
                case EAST  -> "East  (+X)";
                case WEST  -> "West  (-X)";
                default    -> dir.getName();
            };
            lines.add("Facing: " + dirName);
        }

        // Speed
        if (cfg.hudShowSpeed) {
            double dx = player.getX() - player.prevX;
            double dz = player.getZ() - player.prevZ;
            double speed = Math.sqrt(dx * dx + dz * dz) * 20; // blocks/sec
            lines.add(String.format("Speed: %.2f b/s", speed));
        }
        return lines;
    }

    private static void renderArmorHud(DrawContext ctx, ClientPlayerEntity player) {
        int screenWidth  = MC.getWindow().getScaledWidth();
        int screenHeight = MC.getWindow().getScaledHeight();

        // Armor slots: head, chest, legs, feet (index 3..0)
        ItemStack[] armor = new ItemStack[4];
        int idx = 0;
        for (ItemStack stack : player.getArmorItems()) {
            armor[idx++] = stack;
        }

        int baseX = screenWidth / 2 - 91; // align with hotbar
        int baseY = screenHeight - 55;

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = armor[i];
            if (stack == null || stack.isEmpty()) continue;

            int slotX = baseX + (3 - i) * 20;

            // Render item icon
            ctx.drawItem(stack, slotX, baseY);

            // Durability text if damaged
            if (stack.isDamageable() && stack.getDamage() > 0) {
                int dur = stack.getMaxDamage() - stack.getDamage();
                int maxDur = stack.getMaxDamage();
                String durText = dur + "";

                // Color: green → yellow → red
                float fraction = (float) dur / maxDur;
                int r = fraction < 0.5f ? 255 : (int) (255 * (1 - fraction) * 2);
                int g = fraction > 0.5f ? 255 : (int) (255 * fraction * 2);
                int durColor = 0xFF000000 | (r << 16) | (g << 8);

                ctx.drawTextWithShadow(MC.textRenderer, durText, slotX, baseY + 9, durColor);
            }
        }
    }
}
