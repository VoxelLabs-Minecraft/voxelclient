package de.voxellabs.voxelclient.client.gui;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Main MyClient settings screen.
 *
 * Navigation:
 *   [HUD]  [Zoom]  [Freelook]  [Cosmetics]
 *
 * Each tab shows relevant toggle buttons / sliders.
 */
public class ClientModScreen extends Screen {

    private final Screen parent;
    private Tab currentTab = Tab.HUD;

    // HUD tab widgets
    private ButtonWidget btnFps, btnCoords, btnArmor, btnDirection, btnSpeed;

    // Zoom tab widgets
    private ButtonWidget btnSmoothZoom;

    // Freelook tab
    private ButtonWidget btnFreelookEnabled;

    // Cosmetics tab
    private ButtonWidget btnCapeEnabled;
    private TextFieldWidget fieldCapeUrl;
    private ButtonWidget btnReloadCape;

    private static final int TAB_Y       = 30;
    private static final int CONTENT_Y   = 70;
    private static final int BTN_W       = 200;
    private static final int BTN_H       = 20;
    private static final int BTN_SPACING = 24;

    private enum Tab { HUD, ZOOM, FREELOOK, COSMETICS }

    public ClientModScreen(Screen parent) {
        super(Text.translatable("myclient.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Tab buttons
        String[] tabNames = {"HUD", "Zoom", "Freelook", "Cosmetics"};
        Tab[] tabs = Tab.values();
        int tabW = 80;
        int totalTabW = tabW * tabNames.length + 4 * (tabNames.length - 1);
        int tabStartX = (this.width - totalTabW) / 2;

        for (int i = 0; i < tabNames.length; i++) {
            final Tab tab = tabs[i];
            addDrawableChild(ButtonWidget.builder(
                            Text.literal(tabNames[i]),
                            btn -> switchTab(tab))
                    .dimensions(tabStartX + i * (tabW + 4), TAB_Y, tabW, 20)
                    .build());
        }

        // Close / Save button
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Save & Close"),
                        btn -> {
                            VoxelClientConfig.save();
                            this.client.setScreen(parent);
                        })
                .dimensions(this.width / 2 - 75, this.height - 30, 150, 20)
                .build());

        buildTabWidgets();
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        clearChildren();
        init(); // re-run to rebuild buttons
    }

    private void buildTabWidgets() {
        VoxelClientConfig cfg = VoxelClientConfig.get();
        int cx = this.width / 2 - BTN_W / 2;

        switch (currentTab) {
            case HUD -> {
                int y = CONTENT_Y;
                btnFps = addToggle(cx, y, "FPS Counter", cfg.hudShowFps,
                        btn -> { cfg.hudShowFps = !cfg.hudShowFps; updateToggleText(btn, "FPS Counter", cfg.hudShowFps); });
                y += BTN_SPACING;
                btnCoords = addToggle(cx, y, "Coordinates", cfg.hudShowCoords,
                        btn -> { cfg.hudShowCoords = !cfg.hudShowCoords; updateToggleText(btn, "Coordinates", cfg.hudShowCoords); });
                y += BTN_SPACING;
                btnArmor = addToggle(cx, y, "Armor Durability", cfg.hudShowArmor,
                        btn -> { cfg.hudShowArmor = !cfg.hudShowArmor; updateToggleText(btn, "Armor Durability", cfg.hudShowArmor); });
                y += BTN_SPACING;
                btnDirection = addToggle(cx, y, "Direction", cfg.hudShowDirection,
                        btn -> { cfg.hudShowDirection = !cfg.hudShowDirection; updateToggleText(btn, "Direction", cfg.hudShowDirection); });
                y += BTN_SPACING;
                btnSpeed = addToggle(cx, y, "Speed", cfg.hudShowSpeed,
                        btn -> { cfg.hudShowSpeed = !cfg.hudShowSpeed; updateToggleText(btn, "Speed", cfg.hudShowSpeed); });
            }

            case ZOOM -> {
                int y = CONTENT_Y;
                btnSmoothZoom = addToggle(cx, y, "Smooth Zoom", cfg.zoomSmoothZoom,
                        btn -> { cfg.zoomSmoothZoom = !cfg.zoomSmoothZoom; updateToggleText(btn, "Smooth Zoom", cfg.zoomSmoothZoom); });
                y += BTN_SPACING;
                // Zoom FOV decrease/increase buttons
                addDrawableChild(ButtonWidget.builder(Text.literal("Zoom FOV: " + (int) cfg.zoomFov + "°  [−]"),
                                btn -> { cfg.zoomFov = Math.max(1, cfg.zoomFov - 1); btn.setMessage(Text.literal("Zoom FOV: " + (int) cfg.zoomFov + "°  [−]")); })
                        .dimensions(cx, y, BTN_W / 2 - 2, BTN_H).build());
                addDrawableChild(ButtonWidget.builder(Text.literal("[+]"),
                                btn -> { cfg.zoomFov = Math.min(30, cfg.zoomFov + 1); })
                        .dimensions(cx + BTN_W / 2 + 2, y, BTN_W / 2 - 2, BTN_H).build());
            }

            case FREELOOK -> {
                int y = CONTENT_Y;
                btnFreelookEnabled = addToggle(cx, y, "Freelook", cfg.freelookEnabled,
                        btn -> { cfg.freelookEnabled = !cfg.freelookEnabled; updateToggleText(btn, "Freelook", cfg.freelookEnabled); });
            }

            case COSMETICS -> {
                int y = CONTENT_Y;
                btnCapeEnabled = addToggle(cx, y, "Custom Cape", cfg.capeEnabled,
                        btn -> { cfg.capeEnabled = !cfg.capeEnabled; updateToggleText(btn, "Custom Cape", cfg.capeEnabled); });
                y += BTN_SPACING;

                addDrawableChild(ButtonWidget.builder(Text.literal("Cape URL:"), btn -> {})
                        .dimensions(cx, y, BTN_W, BTN_H)
                        .build()).active = false;
                y += BTN_SPACING;

                fieldCapeUrl = addDrawableChild(new TextFieldWidget(
                        this.textRenderer, cx, y, BTN_W, BTN_H, Text.literal("Cape URL")));
                fieldCapeUrl.setMaxLength(512);
                fieldCapeUrl.setText(cfg.capeUrl);
                fieldCapeUrl.setChangedListener(text -> cfg.capeUrl = text);
                y += BTN_SPACING;

                btnReloadCape = addDrawableChild(ButtonWidget.builder(
                                Text.literal("Reload Cape"),
                                btn -> {
                                    cfg.capeUrl = fieldCapeUrl.getText();
                                    CosmeticsManager.reloadCapeFromConfig();
                                })
                        .dimensions(cx, y, BTN_W, BTN_H)
                        .tooltip(Tooltip.of(Text.literal("Downloads and applies cape from the URL above.")))
                        .build());

                y += BTN_SPACING;
                // Status
                String status = CosmeticsManager.isCapeLoaded()  ? "§aCape loaded"
                        : CosmeticsManager.isCapeLoading() ? "§eLoading…"
                        : "§cNo cape";
                // rendered in render()
            }
        }
    }

    private ButtonWidget addToggle(int x, int y, String label, boolean value, ButtonWidget.PressAction action) {
        return addDrawableChild(ButtonWidget.builder(
                        Text.literal(label + ": " + (value ? "§aON" : "§cOFF")), action)
                .dimensions(x, y, BTN_W, BTN_H)
                .build());
    }

    private void updateToggleText(ButtonWidget btn, String label, boolean value) {
        btn.setMessage(Text.literal(label + ": " + (value ? "§aON" : "§cOFF")));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        // Title
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        // Tab indicator
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7── " + currentTab.name() + " ──"),
                this.width / 2, TAB_Y + 25, 0xAAAAAA);

        // Cosmetics status
        if (currentTab == Tab.COSMETICS) {
            String status = CosmeticsManager.isCapeLoaded()  ? "§aCape loaded ✔"
                    : CosmeticsManager.isCapeLoading() ? "§eLoading…"
                    : "§cNo cape active";
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status),
                    this.width / 2, this.height - 50, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        VoxelClientConfig.save();
        this.client.setScreen(parent);
    }
}
