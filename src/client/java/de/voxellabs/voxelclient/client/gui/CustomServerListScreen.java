package de.voxellabs.voxelclient.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Server List Screen.
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │                     Server List                                  │
 * │                                                                  │
 * │  ★ [PINNED] Plantaria.net   ────────── play.plantaria.net        │
 * │  ★ [PINNED] ave.rip Network ────────── play.ave.rip              │
 * │  ─────────────────────────────────────────────────────           │
 * │  [User Server 1]                                                 │
 * │  [User Server 2]                                                 │
 * │  ...                                                             │
 * │                                                                  │
 * │   [ Add Server ]  [ Edit ]  [ Remove ]  [ Connect ]  [ Back ]   │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * Rules:
 *  • Pinned servers are ALWAYS at the top (index 0 & 1).
 *  • Pinned servers have no Edit / Remove button.
 *  • User-added servers can be freely managed below.
 */
public class CustomServerListScreen extends Screen {

    // ── Pinned Servers (immutable) ────────────────────────────────────────────
    private static final PinnedServer[] PINNED = {
            new PinnedServer(
                    "ave.rip Network",
                    "ave.rip",
                    "§bCommunity • PvP • Mini-Games",
                    0xFF3498DB   // blue accent
            ),
        new PinnedServer(
            "Plantaria.net",
            "plantaria.net",
            "§aEuropes #1 Survival Network",
            0xFF2ECC71   // green accent
        )
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final List<UserServer> userServers = new ArrayList<>();
    private int selectedIndex = -1; // -1 = nothing, 0..PINNED.length-1 = pinned, then user

    // UI layout constants
    private static final int ENTRY_H     = 36;
    private static final int LIST_X      = 0;      // filled dynamically
    private static final int LIST_Y      = 32;
    private static final int BTN_H       = 20;
    private static final int PINNED_ACCENT_W = 4;

    // Input fields for "Add Server" dialog (shown inline when adding)
    private boolean addingServer       = false;
    private String  inputName          = "";
    private String  inputAddress       = "";
    private boolean editingName        = true; // which field cursor is in

    // Scroll offset
    private int scrollOffset = 0;

    // ── Constructor ───────────────────────────────────────────────────────────
    public CustomServerListScreen(Screen parent) {
        super(Text.literal("Server List"));
        this.parent = parent;
        loadUserServers();
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        int btnY = this.height - 28;
        int cx   = this.width / 2;

        if (addingServer) {
            initAddServerWidgets(cx, btnY);
        } else {
            initMainWidgets(cx, btnY);
        }
    }

    private void initMainWidgets(int cx, int btnY) {
        // Add Server
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Server"),
                btn -> startAddServer())
            .dimensions(cx - 152, btnY, 100, BTN_H).build());

        // Edit (disabled for pinned)
        ButtonWidget editBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Edit"),
                btn -> editSelectedServer())
            .dimensions(cx - 48, btnY, 50, BTN_H).build());
        editBtn.active = isUserServerSelected();

        // Remove (disabled for pinned)
        ButtonWidget removeBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Remove"),
                btn -> removeSelectedServer())
            .dimensions(cx + 6, btnY, 50, BTN_H).build());
        removeBtn.active = isUserServerSelected();

        // Connect
        ButtonWidget connectBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Connect"),
                btn -> connectToSelected())
            .dimensions(cx + 60, btnY, 92, BTN_H).build());
        connectBtn.active = selectedIndex >= 0;

        // Back
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"),
                btn -> this.client.setScreen(parent))
            .dimensions(this.width - 56, btnY, 52, BTN_H).build());
    }

    private void initAddServerWidgets(int cx, int btnY) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Add ✔"),
                btn -> confirmAddServer())
            .dimensions(cx - 52, btnY, 50, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                btn -> { addingServer = false; clearChildren(); init(); })
            .dimensions(cx + 2, btnY, 50, BTN_H).build());
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dark background
        ctx.fillGradient(0, 0, this.width, this.height, 0xFF0d0d1a, 0xFF08080f);

        // Title
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§f⚡  Server List"), this.width / 2, 10, 0xFFFFFF);

        // Divider under title
        ctx.fill(0, 28, this.width, 29, 0x44FFFFFF);

        // Server entries
        if (addingServer) {
            renderAddServerForm(ctx, mouseX, mouseY);
        } else {
            renderServerList(ctx, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderServerList(DrawContext ctx, int mouseX, int mouseY) {
        int entryW = this.width - 20;
        int entryX = 10;
        int y      = LIST_Y - scrollOffset;

        // ── Pinned servers ────────────────────────────────────────────────────
        for (int i = 0; i < PINNED.length; i++) {
            PinnedServer ps = PINNED[i];
            boolean hovered  = mouseX >= entryX && mouseX <= entryX + entryW
                            && mouseY >= y     && mouseY <= y + ENTRY_H;
            boolean selected = (selectedIndex == i);

            renderPinnedEntry(ctx, ps, entryX, y, entryW, hovered, selected);
            y += ENTRY_H + 2;
        }

        // Separator between pinned and user servers
        int sepY = y + 2;
        ctx.fill(entryX + 20, sepY, entryX + entryW - 20, sepY + 1, 0x33FFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§8── Your Servers ──"),
                this.width / 2, sepY + 4, 0x555555);
        y += 16;

        // ── User servers ──────────────────────────────────────────────────────
        for (int i = 0; i < userServers.size(); i++) {
            UserServer us   = userServers.get(i);
            int globalIdx   = PINNED.length + i;
            boolean hovered = mouseX >= entryX && mouseX <= entryX + entryW
                           && mouseY >= y      && mouseY <= y + ENTRY_H;
            boolean selected = (selectedIndex == globalIdx);

            renderUserEntry(ctx, us, entryX, y, entryW, hovered, selected);
            y += ENTRY_H + 2;
        }

        if (userServers.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§7No servers added yet. Click §fAdd Server§7."),
                    this.width / 2, y + 8, 0x777777);
        }
    }

    private void renderPinnedEntry(DrawContext ctx, PinnedServer ps,
                                   int x, int y, int w,
                                   boolean hovered, boolean selected) {
        // Background
        int bg = selected ? 0x88334466 : hovered ? 0x55223355 : 0x44111122;
        ctx.fill(x, y, x + w, y + ENTRY_H, bg);

        // Accent bar on the left (colour per server)
        ctx.fill(x, y, x + PINNED_ACCENT_W, y + ENTRY_H, ps.accentColor);

        // ★ Pinned badge
        ctx.drawTextWithShadow(this.textRenderer, "★ PINNED",
                x + 8, y + 3, 0xFFD700);

        // Server name
        ctx.drawTextWithShadow(this.textRenderer, ps.displayName,
                x + 8, y + 13, 0xFFFFFF);

        // Address
        ctx.drawTextWithShadow(this.textRenderer, "§8" + ps.address,
                x + 8, y + 24, 0xAAAAAA);

        // Tagline (right side)
        ctx.drawTextWithShadow(this.textRenderer, ps.tagline,
                x + w - this.textRenderer.getWidth(ps.tagline) - 6, y + 13, 0xFFFFFF);

        // Border glow when selected
        if (selected) {
            ctx.fill(x, y, x + w, y + 1, ps.accentColor);
            ctx.fill(x, y + ENTRY_H - 1, x + w, y + ENTRY_H, ps.accentColor);
        }
    }

    private void renderUserEntry(DrawContext ctx, UserServer us,
                                 int x, int y, int w,
                                 boolean hovered, boolean selected) {
        int bg = selected ? 0x88443333 : hovered ? 0x55332222 : 0x33111111;
        ctx.fill(x, y, x + w, y + ENTRY_H, bg);
        ctx.fill(x, y, x + PINNED_ACCENT_W, y + ENTRY_H, 0xFF888888);

        ctx.drawTextWithShadow(this.textRenderer, us.name,
                x + 8, y + 10, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "§8" + us.address,
                x + 8, y + 22, 0xAAAAAA);

        if (selected) {
            ctx.fill(x, y, x + w, y + 1, 0xFFAAAAAA);
            ctx.fill(x, y + ENTRY_H - 1, x + w, y + ENTRY_H, 0xFFAAAAAA);
        }
    }

    private void renderAddServerForm(DrawContext ctx, int mouseX, int mouseY) {
        int cx = this.width / 2;
        int formY = this.height / 2 - 50;

        // Panel
        ctx.fill(cx - 160, formY - 10, cx + 160, formY + 80, 0xCC111122);
        ctx.fill(cx - 160, formY - 10, cx + 160, formY - 9, 0xFF3355AA);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§eAdd New Server"), cx, formY, 0xFFFFFF);

        // Name field
        boolean nameFocused = editingName;
        ctx.fill(cx - 120, formY + 16, cx + 120, formY + 28,
                nameFocused ? 0x99334488 : 0x66222244);
        ctx.fill(cx - 120, formY + 15, cx + 120, formY + 16,
                nameFocused ? 0xFFAABBFF : 0xFF555577);
        String nameDisplay = inputName.isEmpty() && !nameFocused
                ? "§7Server Name..." : inputName + (nameFocused ? "§f|" : "");
        ctx.drawTextWithShadow(this.textRenderer, nameDisplay, cx - 116, formY + 18, 0xFFFFFF);

        // Address field
        boolean addrFocused = !editingName;
        ctx.fill(cx - 120, formY + 38, cx + 120, formY + 50,
                addrFocused ? 0x99334488 : 0x66222244);
        ctx.fill(cx - 120, formY + 37, cx + 120, formY + 38,
                addrFocused ? 0xFFAABBFF : 0xFF555577);
        String addrDisplay = inputAddress.isEmpty() && !addrFocused
                ? "§7Server Address..." : inputAddress + (addrFocused ? "§f|" : "");
        ctx.drawTextWithShadow(this.textRenderer, addrDisplay, cx - 116, formY + 40, 0xFFFFFF);

        ctx.drawTextWithShadow(this.textRenderer, "§7[TAB] switch field • [ENTER] confirm",
                cx - 116, formY + 56, 0x555555);
    }

    // ── Mouse / Keyboard input ────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!addingServer) {
            handleListClick(mouseX, mouseY);
        } else {
            int cx = this.width / 2;
            int formY = this.height / 2 - 50;
            if (mouseY >= formY + 16 && mouseY <= formY + 28) editingName = true;
            if (mouseY >= formY + 38 && mouseY <= formY + 50) editingName = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleListClick(double mouseX, double mouseY) {
        int entryW = this.width - 20;
        int entryX = 10;
        int y      = LIST_Y - scrollOffset;

        for (int i = 0; i < PINNED.length; i++) {
            if (mouseX >= entryX && mouseX <= entryX + entryW
             && mouseY >= y      && mouseY <= y + ENTRY_H) {
                selectedIndex = i;
                clearChildren(); init(); // refresh button states
                return;
            }
            y += ENTRY_H + 2;
        }
        y += 16; // separator

        for (int i = 0; i < userServers.size(); i++) {
            if (mouseX >= entryX && mouseX <= entryX + entryW
             && mouseY >= y      && mouseY <= y + ENTRY_H) {
                int globalIdx = PINNED.length + i;
                if (selectedIndex == globalIdx) {
                    // Double-click → connect
                    connectToSelected();
                } else {
                    selectedIndex = globalIdx;
                    clearChildren(); init();
                }
                return;
            }
            y += ENTRY_H + 2;
        }

        selectedIndex = -1;
        clearChildren(); init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (addingServer) {
            handleAddServerKey(keyCode);
            return true;
        }
        // ENTER → connect to selected
        if (keyCode == 257 && selectedIndex >= 0) { // 257 = ENTER
            connectToSelected();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleAddServerKey(int keyCode) {
        switch (keyCode) {
            case 258 -> editingName = !editingName; // TAB
            case 257 -> confirmAddServer();          // ENTER
            case 259 -> {                            // BACKSPACE
                if (editingName) {
                    if (!inputName.isEmpty())
                        inputName = inputName.substring(0, inputName.length() - 1);
                } else {
                    if (!inputAddress.isEmpty())
                        inputAddress = inputAddress.substring(0, inputAddress.length() - 1);
                }
            }
            case 256 -> { addingServer = false; clearChildren(); init(); } // ESC
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (addingServer) {
            if (editingName)  inputName    += chr;
            else              inputAddress += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, scrollOffset - (int)(verticalAmount * 8));
        return true;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void connectToSelected() {
        if (selectedIndex < 0) return;

        String addr;
        String name;

        if (selectedIndex < PINNED.length) {
            addr = PINNED[selectedIndex].address;
            name = PINNED[selectedIndex].displayName;
        } else {
            UserServer us = userServers.get(selectedIndex - PINNED.length);
            addr = us.address;
            name = us.name;
        }

        ServerInfo info = new ServerInfo(name, addr, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(addr), info, false, null);
    }

    private void startAddServer() {
        inputName    = "";
        inputAddress = "";
        editingName  = true;
        addingServer = true;
        clearChildren();
        init();
    }

    private void confirmAddServer() {
        String n = inputName.trim();
        String a = inputAddress.trim();
        if (!n.isEmpty() && !a.isEmpty()) {
            userServers.add(new UserServer(n, a));
            saveUserServers();
        }
        addingServer = false;
        clearChildren();
        init();
    }

    private void editSelectedServer() {
        if (!isUserServerSelected()) return;
        UserServer us = userServers.get(selectedIndex - PINNED.length);
        inputName    = us.name;
        inputAddress = us.address;
        editingName  = true;
        addingServer = true;
        // Remove old entry; confirmAddServer will re-add
        userServers.remove(selectedIndex - PINNED.length);
        selectedIndex = -1;
        clearChildren();
        init();
    }

    private void removeSelectedServer() {
        if (!isUserServerSelected()) return;
        userServers.remove(selectedIndex - PINNED.length);
        selectedIndex = -1;
        saveUserServers();
        clearChildren();
        init();
    }

    private boolean isUserServerSelected() {
        return selectedIndex >= PINNED.length;
    }

    // ── Persistence ───────────────────────────────────────────────────────────
    /**
     * Loads user-added servers from the vanilla server list (servers.dat).
     * For simplicity here we just pre-fill with a couple of examples.
     * A full implementation would read/write servers.dat via ServerList.
     */
    private void loadUserServers() {
        // TODO: integrate with net.minecraft.client.network.ServerList
        // to persist user servers to servers.dat so they survive restarts.
        //
        // Example:
        //   ServerList list = new ServerList(MinecraftClient.getInstance());
        //   list.loadFile();
        //   for (int i = 0; i < list.size(); i++) userServers.add(...);
    }

    private void saveUserServers() {
        // TODO: write back to servers.dat
    }

    // ── Data classes ──────────────────────────────────────────────────────────
    private static final class PinnedServer {
        final String displayName;
        final String address;
        final String tagline;
        final int    accentColor;

        PinnedServer(String displayName, String address, String tagline, int accentColor) {
            this.displayName = displayName;
            this.address     = address;
            this.tagline     = tagline;
            this.accentColor = accentColor;
        }
    }

    private static final class UserServer {
        String name;
        String address;

        UserServer(String name, String address) {
            this.name    = name;
            this.address = address;
        }
    }
}
