package de.voxellabs.voxelclient.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Base64;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomServerListScreen extends Screen {

    // ── Pinned Servers ────────────────────────────────────────────────────────
    private static final PinnedServer[] PINNED = {
            new PinnedServer("ave.rip Network",  "ave.rip",       "§cPlay • Fight • Conquer", 0xFF3498DB),
            new PinnedServer("Plantaria.net",    "plantaria.net", "§aRoots • Worlds • Adventure",  0xFF2ECC71)
    };

    // ── MOTD Cache ────────────────────────────────────────────────────────────
    private static final Map<String, String> MOTD_CACHE    = new ConcurrentHashMap<>();
    private static final Map<String, String> PLAYERS_CACHE  = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> FAVICON_CACHE = new ConcurrentHashMap<>();

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final List<UserServer> userServers = new ArrayList<>();
    private int selectedIndex = -1;

    private static final int ENTRY_H        = 36;
    private static final int LIST_Y         = 32;
    private static final int BTN_H          = 20;
    private static final int PINNED_ACCENT_W = 4;

    private boolean addingServer = false;
    private boolean isEditing     = false;
    private int     editingIndex  = -1;  // Index des Servers der gerade bearbeitet wird
    private String  inputName    = "";
    private String  inputAddress = "";
    private boolean editingName  = true;
    private int     scrollOffset = 0;

    // ── Constructor ───────────────────────────────────────────────────────────
    public CustomServerListScreen(Screen parent) {
        super(Text.literal("Server List"));
        this.parent = parent;
        loadUserServers();
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        // Ping alle Server asynchron für MOTD
        for (PinnedServer ps : PINNED) pingServerAsync(ps.address);
        for (UserServer   us : userServers) pingServerAsync(us.address);

        int btnY = this.height - 28;
        int cx   = this.width  / 2;
        if (addingServer) initAddServerWidgets(cx, btnY);
        else              initMainWidgets(cx, btnY);
    }

    private void initMainWidgets(int cx, int btnY) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Server"),
                        btn -> startAddServer())
                .dimensions(cx - 152, btnY, 100, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"),
                        btn -> refreshServers())
                .dimensions(this.width - 112, btnY, 52, BTN_H).build());

        ButtonWidget editBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Edit"),
                        btn -> editSelectedServer())
                .dimensions(cx - 48, btnY, 50, BTN_H).build());
        editBtn.active = isUserServerSelected();

        ButtonWidget removeBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Remove"),
                        btn -> removeSelectedServer())
                .dimensions(cx + 6, btnY, 50, BTN_H).build());
        removeBtn.active = isUserServerSelected();

        ButtonWidget connectBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Connect"),
                        btn -> connectToSelected())
                .dimensions(cx + 60, btnY, 92, BTN_H).build());
        connectBtn.active = selectedIndex >= 0;

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"),
                        btn -> this.client.setScreen(new CustomMainMenuScreen()))
                .dimensions(this.width - 56, btnY, 52, BTN_H).build());
    }

    private void initAddServerWidgets(int cx, int btnY) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Add ✔"),
                        btn -> confirmAddServer())
                .dimensions(cx - 52, btnY, 50, BTN_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                        btn -> { addingServer = false; isEditing = false; editingIndex = -1; clearChildren(); init(); })
                .dimensions(cx + 2, btnY, 50, BTN_H).build());
    }

    // ── Background ────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0xFF0d0d1a, 0xFF08080f);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§f⚡  Server List"), this.width / 2, 10, 0xFFFFFF);
        ctx.fill(0, 28, this.width, 29, 0x44FFFFFF);

        if (addingServer) renderAddServerForm(ctx, mouseX, mouseY);
        else              renderServerList(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void refreshServers() {
        MOTD_CACHE.clear();
        PLAYERS_CACHE.clear();

        for (PinnedServer ps : PINNED) pingServerAsync(ps.address);
        for (UserServer us : userServers) pingServerAsync(us.address);
    }

    private void renderServerList(DrawContext ctx, int mouseX, int mouseY) {
        int entryW = this.width - 20;
        int entryX = 10;
        int y      = LIST_Y - scrollOffset;

        // Pinned
        for (int i = 0; i < PINNED.length; i++) {
            PinnedServer ps      = PINNED[i];
            boolean hovered      = isHovered(mouseX, mouseY, entryX, y, entryW, ENTRY_H);
            boolean selected     = (selectedIndex == i);
            String motd          = MOTD_CACHE.getOrDefault(ps.address, ps.tagline);
            String players       = PLAYERS_CACHE.get(ps.address);
            renderPinnedEntry(ctx, ps, entryX, y, entryW, hovered, selected, motd, players);
            y += ENTRY_H + 2;
        }

        // Separator
        int sepY = y + 2;
        ctx.fill(entryX + 20, sepY, entryX + entryW - 20, sepY + 1, 0x33FFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§8── Your Servers ──"), this.width / 2, sepY + 4, 0x555555);
        y += 16;

        // User servers
        for (int i = 0; i < userServers.size(); i++) {
            UserServer us    = userServers.get(i);
            int globalIdx    = PINNED.length + i;
            boolean hovered  = isHovered(mouseX, mouseY, entryX, y, entryW, ENTRY_H);
            boolean selected = (selectedIndex == globalIdx);
            String motd      = MOTD_CACHE.getOrDefault(us.address, "§7...");
            String players   = PLAYERS_CACHE.get(us.address);
            renderUserEntry(ctx, us, entryX, y, entryW, hovered, selected, motd, players);
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
                                   boolean hovered, boolean selected,
                                   String motd, String players) {
        int bg = selected ? 0xCC334466 : hovered ? 0xAA223355 : 0x88111133;
        ctx.fill(x, y, x + w, y + ENTRY_H, bg);
        ctx.fill(x, y, x + PINNED_ACCENT_W, y + ENTRY_H, ps.accentColor);

        // Server Icon (32x32 falls vorhanden, sonst Star)
        Identifier favicon = FAVICON_CACHE.get(ps.address);
        int textStartX = x + 8;
        if (favicon != null) {
            ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                    favicon, x + 6, y + 2, 0, 0, 32, 32, 32, 32);
            textStartX = x + 42;
        }

        ctx.drawTextWithShadow(this.textRenderer, "§6★ §eFEATURED", textStartX, y + 3, 0xFFD700);
        ctx.drawTextWithShadow(this.textRenderer, "§f" + ps.displayName, textStartX, y + 13, 0xFFFFFF);

        // MOTD (eine Zeile, Farben aus Server)
        String displayMotd = motd != null ? motd : ps.tagline;
        ctx.drawTextWithShadow(this.textRenderer, displayMotd, textStartX, y + 24, 0xBBBBBB);

        // Players + address rechts
        String right = (players != null ? "§a" + players + " §8| " : "") + "§8" + ps.address;
        ctx.drawTextWithShadow(this.textRenderer, right,
                x + w - this.textRenderer.getWidth(right) - 6, y + 13, 0xFFFFFF);

        if (selected) {
            ctx.fill(x, y, x + w, y + 1, ps.accentColor);
            ctx.fill(x, y + ENTRY_H - 1, x + w, y + ENTRY_H, ps.accentColor);
        }
        ctx.fill(x, y + ENTRY_H, x + w, y + ENTRY_H + 1, 0x33FFFFFF);
    }

    private void renderUserEntry(DrawContext ctx, UserServer us,
                                 int x, int y, int w,
                                 boolean hovered, boolean selected,
                                 String motd, String players) {
        int bg = selected ? 0x88443333 : hovered ? 0x55332222 : 0x33111111;
        ctx.fill(x, y, x + w, y + ENTRY_H, bg);
        ctx.fill(x, y, x + PINNED_ACCENT_W, y + ENTRY_H, 0xFF888888);

        // Server Icon
        Identifier faviconU = FAVICON_CACHE.get(us.address);
        int userTextX = x + 8;
        if (faviconU != null) {
            ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                    faviconU, x + 6, y + 2, 0, 0, 32, 32, 32, 32);
            userTextX = x + 42;
        }

        ctx.drawTextWithShadow(this.textRenderer, us.name, userTextX, y + 5, 0xFFFFFF);

        // MOTD
        String displayMotd = motd != null ? motd : "§7...";
        ctx.drawTextWithShadow(this.textRenderer, displayMotd, userTextX, y + 17, 0xAAAAAA);

        // Players + address rechts
        String right = (players != null ? "§a" + players + " §8| " : "") + "§8" + us.address;
        ctx.drawTextWithShadow(this.textRenderer, right,
                x + w - this.textRenderer.getWidth(right) - 6, y + 5, 0xFFFFFF);

        if (selected) {
            ctx.fill(x, y, x + w, y + 1, 0xFFAAAAAA);
            ctx.fill(x, y + ENTRY_H - 1, x + w, y + ENTRY_H, 0xFFAAAAAA);
        }
    }

    private void renderAddServerForm(DrawContext ctx, int mouseX, int mouseY) {
        int cx    = this.width  / 2;
        int formY = this.height / 2 - 60;

        ctx.fill(cx - 160, formY - 14, cx + 160, formY + 100, 0xDD111122);
        ctx.fill(cx - 160, formY - 14, cx + 160, formY - 12, 0xFF3355AA);
        ctx.fill(cx - 160, formY - 14, cx - 158, formY + 100, 0xFF3355AA);
        ctx.fill(cx + 158, formY - 14, cx + 160, formY + 100, 0xFF3355AA);
        ctx.fill(cx - 160, formY + 98, cx + 160, formY + 100, 0xFF3355AA);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(isEditing ? "§e✦ Edit Server" : "§e✦ Add New Server"), cx, formY - 4, 0xFFFFFF);

        ctx.drawTextWithShadow(this.textRenderer, "§fServer Name:", cx - 140, formY + 14, 0xFFFFFF);
        boolean nameFocused = editingName;
        ctx.fill(cx - 140, formY + 26, cx + 140, formY + 42, 0xFF000000);
        ctx.fill(cx - 139, formY + 27, cx + 139, formY + 41, nameFocused ? 0xFF1a1a3a : 0xFF0d0d1a);
        int nameBorderColor = nameFocused ? 0xFF5577DD : 0xFF333355;
        ctx.fill(cx - 140, formY + 26, cx + 140, formY + 27, nameBorderColor);
        ctx.fill(cx - 140, formY + 41, cx + 140, formY + 42, nameBorderColor);
        ctx.fill(cx - 140, formY + 26, cx - 139, formY + 42, nameBorderColor);
        ctx.fill(cx + 139, formY + 26, cx + 140, formY + 42, nameBorderColor);
        String nameDisplay = inputName.isEmpty() && !nameFocused
                ? "§7Enter server name..." : inputName + (nameFocused ? "§f_" : "");
        ctx.drawTextWithShadow(this.textRenderer, nameDisplay, cx - 135, formY + 31, 0xFFFFFF);

        ctx.drawTextWithShadow(this.textRenderer, "§fServer Address:", cx - 140, formY + 50, 0xFFFFFF);
        boolean addrFocused = !editingName;
        ctx.fill(cx - 140, formY + 62, cx + 140, formY + 78, 0xFF000000);
        ctx.fill(cx - 139, formY + 63, cx + 139, formY + 77, addrFocused ? 0xFF1a1a3a : 0xFF0d0d1a);
        int addrBorderColor = addrFocused ? 0xFF5577DD : 0xFF333355;
        ctx.fill(cx - 140, formY + 62, cx + 140, formY + 63, addrBorderColor);
        ctx.fill(cx - 140, formY + 77, cx + 140, formY + 78, addrBorderColor);
        ctx.fill(cx - 140, formY + 62, cx - 139, formY + 78, addrBorderColor);
        ctx.fill(cx + 139, formY + 62, cx + 140, formY + 78, addrBorderColor);
        String addrDisplay = inputAddress.isEmpty() && !addrFocused
                ? "§7e.g. play.example.com" : inputAddress + (addrFocused ? "§f_" : "");
        ctx.drawTextWithShadow(this.textRenderer, addrDisplay, cx - 135, formY + 67, 0xFFFFFF);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7[TAB] Switch field  §8•  §7[ENTER] Confirm  §8•  §7[ESC] Cancel"),
                cx, formY + 84, 0x777777);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!addingServer) {
            // Nur Server-Liste anklicken wenn NICHT im Button-Bereich unten
            // (sonst werden Buttons gelöscht bevor super.mouseClicked() sie auslöst)
            int btnY = this.height - 28;
            if (mouseY < btnY - 2) {
                handleListClick(mouseX, mouseY);
            }
        } else {
            int cx    = this.width  / 2;
            int formY = this.height / 2 - 60;
            if (mouseY >= formY + 26 && mouseY <= formY + 42) editingName = true;
            if (mouseY >= formY + 62 && mouseY <= formY + 78) editingName = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleListClick(double mouseX, double mouseY) {
        int entryW = this.width - 20;
        int entryX = 10;
        int y      = LIST_Y - scrollOffset;

        // Pinned — Doppelklick connect
        for (int i = 0; i < PINNED.length; i++) {
            if (isHovered(mouseX, mouseY, entryX, y, entryW, ENTRY_H)) {
                if (selectedIndex == i) {
                    connectToSelected();
                } else {
                    selectedIndex = i;
                    clearChildren(); init();
                }
                return;
            }
            y += ENTRY_H + 2;
        }
        y += 16; // separator

        // User servers — Doppelklick connect
        for (int i = 0; i < userServers.size(); i++) {
            if (isHovered(mouseX, mouseY, entryX, y, entryW, ENTRY_H)) {
                int globalIdx = PINNED.length + i;
                if (selectedIndex == globalIdx) {
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

    private boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (addingServer) { handleAddServerKey(keyCode); return true; }
        if (keyCode == 257 && selectedIndex >= 0) { connectToSelected(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleAddServerKey(int keyCode) {
        switch (keyCode) {
            case 258 -> editingName = !editingName;
            case 257 -> confirmAddServer();
            case 259 -> {
                if (editingName) { if (!inputName.isEmpty())    inputName    = inputName.substring(0, inputName.length() - 1); }
                else             { if (!inputAddress.isEmpty()) inputAddress = inputAddress.substring(0, inputAddress.length() - 1); }
            }
            case 256 -> { addingServer = false; isEditing = false; editingIndex = -1; clearChildren(); init(); }
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (addingServer) {
            if (editingName) inputName    += chr;
            else             inputAddress += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        scrollOffset = Math.max(0, scrollOffset - (int)(v * 8));
        return true;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void connectToSelected() {
        if (selectedIndex < 0) return;
        String addr, name;
        if (selectedIndex < PINNED.length) {
            addr = PINNED[selectedIndex].address;
            name = PINNED[selectedIndex].displayName;
        } else {
            UserServer us = userServers.get(selectedIndex - PINNED.length);
            addr = us.address; name = us.name;
        }
        ServerInfo info = new ServerInfo(name, addr, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(addr), info, false, null);
    }

    private void startAddServer() {
        inputName = ""; inputAddress = ""; editingName = true; addingServer = true; isEditing = false;
        clearChildren(); init();
    }

    private void confirmAddServer() {
        String n = inputName.trim(), a = inputAddress.trim();
        if (!n.isEmpty() && !a.isEmpty()) {
            if (isEditing && editingIndex >= 0 && editingIndex < userServers.size()) {
                // Bestehenden Server aktualisieren
                UserServer us = userServers.get(editingIndex);
                MOTD_CACHE.remove(us.address);   // alten Cache leeren
                FAVICON_CACHE.remove(us.address);
                us.name    = n;
                us.address = a;
                pingServerAsync(a);
            } else {
                // Neuen Server hinzufügen
                userServers.add(new UserServer(n, a));
                pingServerAsync(a);
            }
            saveUserServers();
        }
        addingServer = false; isEditing = false; editingIndex = -1; clearChildren(); init();
    }

    private void editSelectedServer() {
        if (!isUserServerSelected()) return;
        int idx = selectedIndex - PINNED.length;
        UserServer us = userServers.get(idx);
        inputName    = us.name;
        inputAddress = us.address;
        editingName  = true;
        addingServer = true;
        isEditing    = true;
        editingIndex = idx;  // Server merken, aber NICHT löschen
        selectedIndex = -1;
        clearChildren(); init();
    }

    private void removeSelectedServer() {
        if (!isUserServerSelected()) return;
        userServers.remove(selectedIndex - PINNED.length);
        selectedIndex = -1; saveUserServers(); clearChildren(); init();
    }

    private boolean isUserServerSelected() { return selectedIndex >= PINNED.length; }

    // ── Server Ping / MOTD ────────────────────────────────────────────────────
    private void pingServerAsync(String address) {
        if (MOTD_CACHE.containsKey(address)) return;
        MOTD_CACHE.put(address, "§7Pinging...");

        Thread thread = new Thread(() -> {
            try {
                String host = address.split(":")[0];
                int    port = address.contains(":") ? Integer.parseInt(address.split(":")[1]) : 25565;

                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 3000);
                    socket.setSoTimeout(3000);

                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    DataInputStream  in  = new DataInputStream(socket.getInputStream());

                    // Handshake
                    byte[] hostBytes = host.getBytes("UTF-8");
                    java.io.ByteArrayOutputStream payloadBuf = new java.io.ByteArrayOutputStream();
                    DataOutputStream payload = new DataOutputStream(payloadBuf);
                    writeVarInt(payload, 764); // 1.21.4
                    writeVarInt(payload, hostBytes.length);
                    payload.write(hostBytes);
                    payload.writeShort(port);
                    writeVarInt(payload, 1);
                    byte[] payloadBytes = payloadBuf.toByteArray();

                    java.io.ByteArrayOutputStream packetBuf = new java.io.ByteArrayOutputStream();
                    DataOutputStream packet = new DataOutputStream(packetBuf);
                    writeVarInt(packet, payloadBytes.length + 1);
                    writeVarInt(packet, 0x00);
                    packet.write(payloadBytes);
                    out.write(packetBuf.toByteArray());

                    // Status request
                    out.write(new byte[]{0x01, 0x00});
                    out.flush();

                    // Antwort
                    readVarInt(in); // length
                    readVarInt(in); // id
                    int jsonLen = readVarInt(in);
                    byte[] jsonBytes = new byte[jsonLen];
                    in.readFully(jsonBytes);
                    parseMotd(address, new String(jsonBytes, "UTF-8"));
                }
            } catch (Exception e) {
                MOTD_CACHE.put(address, "§cOffline");
            }
        });
        thread.setDaemon(true);
        thread.setName("server-ping-" + address);
        thread.start();
    }

    private void parseMotd(String address, String json) {
        try {
            JsonObject obj = new Gson().fromJson(json, JsonObject.class);

            String motd = "§7Unknown";
            if (obj.has("description")) {
                motd = resolveComponent(obj.get("description"));
            }
            // Zweite Zeile auf eine Zeile reduzieren
            motd = motd.replace("\n", " §8| §r");
            MOTD_CACHE.put(address, motd);

            // Favicon laden falls vorhanden
            if (obj.has("favicon")) {
                loadFavicon(address, obj.get("favicon").getAsString());
            }

            if (obj.has("players")) {
                var players = obj.getAsJsonObject("players");
                int online  = players.has("online") ? players.get("online").getAsInt() : 0;
                int max     = players.has("max")    ? players.get("max").getAsInt()    : 0;
                PLAYERS_CACHE.put(address, online + "/" + max);
            }
        } catch (Exception e) {
            MOTD_CACHE.put(address, "§7Server online");
        }
    }

    private void loadFavicon(String address, String base64) {
        try {
            // "data:image/png;base64,<data>" → nur den Base64-Teil
            String data = base64.contains(",") ? base64.split(",", 2)[1] : base64;
            byte[] bytes = Base64.getDecoder().decode(data);

            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(img);

            String id = "voxelclient:server_icon_" + address.replace(".", "_").replace(":", "_");
            Identifier identifier = Identifier.of(id);

            // Muss auf dem Render-Thread registriert werden
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, texture);
                FAVICON_CACHE.put(address, identifier);
            });
        } catch (Exception e) {
            // Kein Favicon → kein Icon, kein Problem
        }
    }

    private String resolveComponent(com.google.gson.JsonElement element) {
        if (element == null) return "";

        if (element.isJsonPrimitive()) return element.getAsString();

        if (element.isJsonObject()) {
            JsonObject o  = element.getAsJsonObject();
            StringBuilder sb = new StringBuilder();

            if (o.has("color"))         sb.append(colorToSection(o.get("color").getAsString()));
            if (o.has("bold")          && o.get("bold").getAsBoolean())          sb.append("§l");
            if (o.has("italic")        && o.get("italic").getAsBoolean())        sb.append("§o");
            if (o.has("underlined")    && o.get("underlined").getAsBoolean())    sb.append("§n");
            if (o.has("strikethrough") && o.get("strikethrough").getAsBoolean()) sb.append("§m");
            if (o.has("obfuscated")    && o.get("obfuscated").getAsBoolean())    sb.append("§k");
            if (o.has("text"))         sb.append(o.get("text").getAsString());

            if (o.has("extra") && o.get("extra").isJsonArray()) {
                for (var child : o.getAsJsonArray("extra")) {
                    sb.append(resolveComponent(child));
                }
            }
            return sb.toString();
        }

        if (element.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (var child : element.getAsJsonArray()) sb.append(resolveComponent(child));
            return sb.toString();
        }

        return "";
    }

    private String colorToSection(String color) {
        return switch (color.toLowerCase()) {
            case "black"        -> "§0";
            case "dark_blue"    -> "§1";
            case "dark_green"   -> "§2";
            case "dark_aqua"    -> "§3";
            case "dark_red"     -> "§4";
            case "dark_purple"  -> "§5";
            case "gold"         -> "§6";
            case "gray"         -> "§7";
            case "dark_gray"    -> "§8";
            case "blue"         -> "§9";
            case "green"        -> "§a";
            case "aqua"         -> "§b";
            case "red"          -> "§c";
            case "light_purple" -> "§d";
            case "yellow"       -> "§e";
            case "white"        -> "§f";
            default             -> "§f";
        };
    }

    private void writeVarInt(DataOutputStream out, int value) throws Exception {
        while ((value & ~0x7F) != 0) { out.write((value & 0x7F) | 0x80); value >>>= 7; }
        out.write(value);
    }

    private int readVarInt(DataInputStream in) throws Exception {
        int result = 0, shift = 0, b;
        do { b = in.read(); result |= (b & 0x7F) << shift; shift += 7; } while ((b & 0x80) != 0);
        return result;
    }

    // ── Persistence ───────────────────────────────────────────────────────────
    private void loadUserServers() {
        Path file = getSaveFile();
        if (!Files.exists(file)) return;
        try {
            JsonArray arr = new Gson().fromJson(Files.readString(file), JsonArray.class);
            if (arr == null) return;
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                userServers.add(new UserServer(o.get("name").getAsString(), o.get("address").getAsString()));
            }
        } catch (Exception e) {
            System.err.println("[VoxelClient] Server laden fehlgeschlagen: " + e.getMessage());
        }
    }

    private void saveUserServers() {
        try {
            JsonArray arr = new JsonArray();
            for (UserServer us : userServers) {
                JsonObject o = new JsonObject();
                o.addProperty("name",    us.name);
                o.addProperty("address", us.address);
                arr.add(o);
            }
            Files.writeString(getSaveFile(), new Gson().toJson(arr));
        } catch (Exception e) {
            System.err.println("[VoxelClient] Server speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    private static Path getSaveFile() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("voxelclient_servers.json");
    }

    // ── Data classes ──────────────────────────────────────────────────────────
    private static final class PinnedServer {
        final String displayName, address, tagline;
        final int    accentColor;
        PinnedServer(String d, String a, String t, int c) {
            displayName = d; address = a; tagline = t; accentColor = c;
        }
    }

    private static final class UserServer {
        String name, address;
        UserServer(String n, String a) { name = n; address = a; }
    }
}