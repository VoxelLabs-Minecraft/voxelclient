package de.voxellabs.voxelclient.client.ui.hud;

import de.voxellabs.voxelclient.client.ui.module.hud.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD-Editor Screen — VoxelClient v0.0.2
 * Ermöglicht das Verschieben aller HUD-Elemente per Drag & Drop.
 * Öffnen: VoxelClient Settings → UI → HUD-Editor
 */
public class HudEditorScreen extends Screen {

    private final Screen parent;

    // Jede HudElement-Instanz repräsentiert ein verschiebbares Element
    private final List<HudElement> elements = new ArrayList<>();
    private HudElement dragging = null;
    private int dragOffsetX, dragOffsetY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD-Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int sw = width;
        int sh = height;

        // Elemente mit aktuellen Positionen laden
        elements.clear();
        elements.add(new HudElement(DraggableHudSystem.KEYSTROKES, "Keystrokes",
                resolveX(KeystrokesHud.posX, sw, 60), resolveY(KeystrokesHud.posY, sh, 80), 60, 80));
        elements.add(new HudElement(DraggableHudSystem.CPS, "CPS",
                resolveX(CpsCounter.posX, sw, 80), resolveY(CpsCounter.posY, sh, 14), 80, 14));
        elements.add(new HudElement(DraggableHudSystem.ARMOR, "Rüstung",
                resolveX(ArmorDurabilityHud.posX, sw, 85), resolveY(ArmorDurabilityHud.posY, sh, 50), 85, 50));
        elements.add(new HudElement(DraggableHudSystem.PING, "Ping",
                resolveX(PingHud.posX, sw, 70), resolveY(PingHud.posY, sh, 14), 70, 14));

        // In init(), nach den bestehenden 4 Elementen:
        elements.add(new HudElement(DraggableHudSystem.HUD_FPS,    "FPS",
                resolveX(FPSHud.posX,    sw, 50), resolveY(FPSHud.posY,    sh, 12), 50, 12));
        //elements.add(new HudElement(DraggableHudSystem.HUD_COORDS, "XYZ",
        //        resolveX(HudRenderer.posX_coords, sw, 120), resolveY(HudRenderer.posY_coords, sh, 12), 120, 12));
        elements.add(new HudElement(DraggableHudSystem.HUD_DIR,    "Facing",
                resolveX(DirectionHud.posX,    sw, 80), resolveY(DirectionHud.posY,    sh, 12), 80, 12));
        elements.add(new HudElement(DraggableHudSystem.HUD_SPEED,  "Speed",
                resolveX(SpeedHud.posX,  sw, 80), resolveY(SpeedHud.posY,  sh, 12), 80, 12));
        //elements.add(new HudElement(DraggableHudSystem.HUD_ARMOR,  "Armor",
        //        resolveX(HudRenderer.posX_armor,  sw, 80), resolveY(HudRenderer.posY_armor,  sh, 20), 80, 20));

        // Buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("✓ Speichern"), btn -> saveAndClose())
                .dimensions(width / 2 - 105, height - 30, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("⟳ Zurücksetzen"), btn -> resetAll())
                .dimensions(width / 2 + 5, height - 30, 100, 20).build());
    }

    private int resolveX(int stored, int screenW, int elemW) {
        return stored < 0 ? screenW - elemW - 5 : stored;
    }

    private int resolveY(int stored, int screenH, int elemH) {
        return stored < 0 ? screenH - elemH - 5 : stored;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Halbtransparenter Hintergrund (Spiel sichtbar)
        // context.fill(0, 0, width, height, 0x55000000);

        // Gitternetz
        drawGrid(context);

        // Hinweis
        String hint = "§7Elemente mit §fLinksklick §7verschieben • §fEsc §7zum Abbrechen";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(hint), width / 2, 8, 0xFFFFFF);

        // HUD-Elemente
        for (HudElement el : elements) {
            renderHudElement(context, el, el == dragging);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawGrid(DrawContext context) {
        int step = 20;
        int gridColor = 0x22FFFFFF;
        for (int x = 0; x < width; x += step) {
            context.fill(x, 0, x + 1, height, gridColor);
        }
        for (int y = 0; y < height; y += step) {
            context.fill(0, y, width, y + 1, gridColor);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    private void renderHudElement(DrawContext context, HudElement el, boolean selected) {
        int border = selected ? 0xFFFFAA00 : 0xFF5A9E6F;
        int bg = selected ? 0x99223344 : 0x88000000;

        context.fill(el.x, el.y, el.x + el.w, el.y + el.h, bg);
        context.drawBorder(el.x, el.y, el.w, el.h, border);

        // Name zentriert
        int textX = el.x + el.w / 2 - textRenderer.getWidth(el.name) / 2;
        int textY = el.y + el.h / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, el.name, textX, textY, 0xFFFFFFFF, true);

        // Koordinaten (klein)
        String cords = el.x + ", " + el.y;
        context.drawText(textRenderer, "§8" + cords, el.x + 2, el.y + el.h - 10, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                HudElement el = elements.get(i);
                if (mouseX >= el.x && mouseX <= el.x + el.w
                        && mouseY >= el.y && mouseY <= el.y + el.h) {
                    dragging = el;
                    dragOffsetX = (int) mouseX - el.x;
                    dragOffsetY = (int) mouseY - el.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            dragging.x = Math.max(0, Math.min((int) mouseX - dragOffsetX, width - dragging.w));
            dragging.y = Math.max(0, Math.min((int) mouseY - dragOffsetY, height - dragging.h));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void saveAndClose() {
        for (HudElement el : elements) {
            DraggableHudSystem.setPosition(el.id, el.x, el.y);
        }
        close();
    }

    private void resetAll() {
        DraggableHudSystem.resetAll();
        init(); // Neu initialisieren mit Standard-Positionen
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    // ─── INNER CLASS ─────────────────────────────────────────────────────────

    private static class HudElement {
        final String id;
        final String name;
        int x, y, w, h;

        HudElement(String id, String name, int x, int y, int w, int h) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
