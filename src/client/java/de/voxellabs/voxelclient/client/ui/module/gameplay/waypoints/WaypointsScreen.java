package de.voxellabs.voxelclient.client.ui.module.gameplay.waypoints;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Wegpunkte-Verwaltungsscreen — VoxelClient v0.0.2
 * Öffnet sich über die VoxelClient Settings-GUI (Tab "Utility").
 */
public class WaypointsScreen extends Screen {

    private final Screen parent;
    private List<Waypoint> waypoints;
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 24;
    private static final int LIST_START_Y = 50;

    // Eingabefelder für neuen Wegpunkt
    private TextFieldWidget nameField;
    private Waypoint selectedWaypoint = null;

    public WaypointsScreen(Screen parent) {
        super(Text.literal("Wegpunkte"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        waypoints = WaypointManager.getForCurrentWorld();

        // Neuer Wegpunkt — Name-Eingabe
        nameField = new TextFieldWidget(textRenderer, width / 2 - 100, height - 60, 160, 20,
                Text.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.literal("Wegpunkt-Name..."));
        addDrawableChild(nameField);

        // + Button
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Hinzufügen"), btn -> addCurrentPos())
                .dimensions(width / 2 + 65, height - 60, 80, 20)
                .build());

        // Schließen
        addDrawableChild(ButtonWidget.builder(Text.literal("✕ Schließen"), btn -> close())
                .dimensions(width - 85, height - 30, 80, 20)
                .build());
    }

    private void addCurrentPos() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Wegpunkt " + (waypoints.size() + 1);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Waypoint wp = new Waypoint(
                name,
                WaypointManager.getCurrentWorld(),
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                0x00FF88 // Standard-Farbe: Türkis
        );
        WaypointManager.addWaypoint(wp);
        waypoints = WaypointManager.getForCurrentWorld();
        nameField.setText("");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        // Titel
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§6Wegpunkte"), width / 2, 18, 0xFFFFFF);

        // Wegpunkt-Liste
        int listHeight = height - LIST_START_Y - 75;
        int y = LIST_START_Y;

        // Hintergrund der Liste
        context.fill(10, y - 2, width - 10, y + listHeight, 0x88000000);

        int maxVisible = listHeight / ENTRY_HEIGHT;
        int startIdx = scrollOffset;
        int endIdx = Math.min(waypoints.size(), startIdx + maxVisible);

        for (int i = startIdx; i < endIdx; i++) {
            Waypoint wp = waypoints.get(i);
            int entryY = y + (i - startIdx) * ENTRY_HEIGHT;
            renderWaypointEntry(context, wp, entryY, mouseX, mouseY, i == startIdx);
        }

        if (waypoints.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§7Keine Wegpunkte für diese Welt."),
                    width / 2, y + listHeight / 2 - 4, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderWaypointEntry(DrawContext context, Waypoint wp, int y,
                                      int mouseX, int mouseY, boolean first) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        boolean hovered = mouseY >= y && mouseY < y + ENTRY_HEIGHT;
        if (hovered) context.fill(10, y, width - 10, y + ENTRY_HEIGHT - 1, 0x44FFFFFF);

        // Farbquadrat
        int color = wp.color | 0xFF000000;
        context.fill(14, y + 6, 22, y + 16, color);

        // Name + Koordinaten
        String coords = String.format("§8%.0f, %.0f, %.0f", wp.x, wp.y, wp.z);
        String dist = "§7" + wp.getFormattedDistance(
                client.player.getX(), client.player.getY(), client.player.getZ());
        if (wp.isDeathWaypoint) {
            context.drawText(textRenderer, "§c☠ " + wp.name, 26, y + 3, 0xFFFFFF, false);
        } else {
            context.drawText(textRenderer, "§f" + wp.name, 26, y + 3, 0xFFFFFF, false);
        }
        context.drawText(textRenderer, coords + "  " + dist, 26, y + 13, 0xFFFFFF, false);

        // Löschen-Button (rechts)
        context.fill(width - 42, y + 4, width - 14, y + ENTRY_HEIGHT - 4, 0x88FF3333);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("✕"),
                width - 28, y + 7, 0xFFFF5555);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Löschen-Klick erkennen
        int listHeight = height - LIST_START_Y - 75;
        int maxVisible = listHeight / ENTRY_HEIGHT;

        for (int i = 0; i < Math.min(waypoints.size(), maxVisible); i++) {
            int entryY = LIST_START_Y + i * ENTRY_HEIGHT;
            // X-Bereich des Löschen-Buttons
            if (mouseX >= width - 42 && mouseX <= width - 14
                    && mouseY >= entryY + 4 && mouseY <= entryY + ENTRY_HEIGHT - 4) {
                Waypoint wp = waypoints.get(i + scrollOffset);
                WaypointManager.removeWaypoint(wp);
                waypoints = WaypointManager.getForCurrentWorld();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listHeight = height - LIST_START_Y - 75;
        int maxVisible = listHeight / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, waypoints.size() - maxVisible);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) verticalAmount, maxScroll));
        return true;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    public Waypoint getSelectedWaypoint() {
        return selectedWaypoint;
    }

    public void setSelectedWaypoint(Waypoint selectedWaypoint) {
        this.selectedWaypoint = selectedWaypoint;
    }
}
