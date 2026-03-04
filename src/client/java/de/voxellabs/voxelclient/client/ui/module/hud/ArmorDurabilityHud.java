package de.voxellabs.voxelclient.client.ui.module.hud;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Armor Durability Warning — VoxelClient v0.0.2
 * Zeigt die Haltbarkeit aller Rüstungsteile an.
 * Warnung (rot) wenn Haltbarkeit unter 20% liegt.
 */
public class ArmorDurabilityHud {

    public static boolean enabled = true;
    public static int posX = 2;
    public static int posY = -1;

    // Schwellenwert für Warnung (20%)
    private static final float WARNING_THRESHOLD = 0.2f;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private static final String[] SLOT_LABELS = {"H", "C", "L", "B"};
    private static final String[] SLOT_NAMES = {"Helm", "Brust", "Beine", "Schuhe"};

    public static void register() {
        HudRenderCallback.EVENT.register(ArmorDurabilityHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled || !VoxelClientConfig.get().hudShowArmor) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int screenH = client.getWindow().getScaledHeight();
        int x = posX;
        int y = posY < 0 ? screenH / 2 - 30 : posY;

        boolean hasAnyArmor = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!client.player.getEquippedStack(slot).isEmpty()) {
                hasAnyArmor = true;
                break;
            }
        }
        if (!hasAnyArmor) return;

        // Hintergrund
        context.fill(x - 2, y - 2, x + 80, y + ARMOR_SLOTS.length * 11 + 2, 0x88000000);

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            EquipmentSlot slot = ARMOR_SLOTS[i];
            ItemStack stack = client.player.getEquippedStack(slot);
            int rowY = y + i * 11;

            if (stack.isEmpty()) {
                context.drawText(client.textRenderer,
                        "§8" + SLOT_NAMES[i] + ": §7—", x, rowY, 0xFFFFFFFF, false);
                continue;
            }

            int maxDamage = stack.getMaxDamage();
            if (maxDamage <= 0) {
                // Unzerstörbar oder kein Haltbarkeits-Item
                context.drawText(client.textRenderer,
                        "§7" + SLOT_NAMES[i] + ": §a∞", x, rowY, 0xFFFFFFFF, false);
                continue;
            }

            int currentDamage = stack.getDamage();
            int remaining = maxDamage - currentDamage;
            float ratio = (float) remaining / maxDamage;

            int color;
            String prefix;
            if (ratio < WARNING_THRESHOLD) {
                color = 0xFFFF4444; // Rot — kritisch
                prefix = "§c⚠ ";
            } else if (ratio < 0.5f) {
                color = 0xFFFFAA00; // Orange — mittel
                prefix = "§6";
            } else {
                color = 0xFF55FF55; // Grün — gut
                prefix = "§a";
            }

            int percent = (int)(ratio * 100);
            String text = "§7" + SLOT_NAMES[i] + ": " + prefix + percent + "%";
            context.drawText(client.textRenderer, text, x, rowY, 0xFFFFFFFF, false);
        }
    }
}
