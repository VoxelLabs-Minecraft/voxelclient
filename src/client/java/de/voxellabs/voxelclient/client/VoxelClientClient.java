package de.voxellabs.voxelclient.client;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager;
import de.voxellabs.voxelclient.client.discord.DiscordRPCManager;
import de.voxellabs.voxelclient.client.features.FreelookFeature;
import de.voxellabs.voxelclient.client.features.ZoomFeature;
import de.voxellabs.voxelclient.client.gui.ClientModScreen;
import de.voxellabs.voxelclient.client.hud.HudRenderer;
import de.voxellabs.voxelclient.client.version.VersionChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class VoxelClientClient implements ClientModInitializer {

    public static final String MOD_VERSION = VersionChecker.CURRENT_VERSION;

    public static KeyBinding keyOpenMenu;
    public static KeyBinding keyZoom;
    public static KeyBinding keyFreeLook;

    @Override
    public void onInitializeClient() {
        System.out.println("[VoxelClient] ▶ Starting VoxelClient v" + MOD_VERSION + " (Minecraft 1.21.x / Fabric)");
        changeIconsInTaskAndWindows();

        //1. Load configuration from disk
        VoxelClientConfig.load();

        //2. Update-Check asynchron
        VersionChecker.checkForUpdate();

        keyOpenMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclient.openMenu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.voxelclient"
        ));

        keyZoom = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclient.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "key.categories.voxelclient"
        ));

        keyFreeLook = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclient.freelook",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "key.categories.voxelclient"
        ));

        HudRenderer.register();

        ZoomFeature.init(keyZoom);
        FreelookFeature.init(keyFreeLook);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyOpenMenu.wasPressed()) {
                client.setScreen(new ClientModScreen(client.currentScreen));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private int delay = 20; // wait 1 second (20 ticks)
            private boolean done = false;

            @Override
            public void onEndTick(MinecraftClient client) {
                if (!done && client.player != null && delay-- <= 0) {
                    CosmeticsManager.reloadCapeFromConfig();
                    done = true;
                }
            }
        });

        // Set window title with branding when not in fullscreen
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private boolean lastFullscreen = false;
            private boolean initialized = false;

            @Override
            public void onEndTick(MinecraftClient client) {
                boolean fullscreen = client.getWindow().isFullscreen();
                if (!initialized || fullscreen != lastFullscreen) {
                    initialized = true;
                    lastFullscreen = fullscreen;
                    if (!fullscreen) {
                        client.getWindow().setTitle("VoxelClient v" + MOD_VERSION);
                    }
                }
            }
        });
        System.out.println("[VoxelClient] ✔ Initialisation complete!");
        System.out.println("[VoxelClient]   Pinned servers: Plantaria.net, ave.rip");
        System.out.println("[VoxelClient]   Update-Check läuft im Hintergrund…");
        System.out.println("[VoxelClient]   Press RIGHT SHIFT in-game to open settings.");

        showDiscordRichPresence();
    }

    private void changeIconsInTaskAndWindows() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try (InputStream is = VoxelClientClient.class
                    .getResourceAsStream("/assets/voxelclient/icon.png")) {

                if (is == null) {
                    System.err.println("[VoxelClient] icon.png nicht gefunden!");
                    return;
                }

                NativeImage img = NativeImage.read(is);
                long handle = client.getWindow().getHandle();

                // RGBA-Bytes aus NativeImage lesen
                int w = img.getWidth();
                int h = img.getHeight();
                ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int pixel = img.getColorArgb(x, y);
                        // NativeImage speichert in ABGR → umwandeln zu RGBA
                        buffer.put((byte)((pixel >> 16) & 0xFF)); // R
                        buffer.put((byte)((pixel >>  8) & 0xFF)); // G
                        buffer.put((byte)((pixel)       & 0xFF)); // B
                        buffer.put((byte)((pixel >> 24) & 0xFF)); // A
                    }
                }
                buffer.flip();

                // GLFW Icon setzen
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    GLFWImage icon = GLFWImage.malloc(stack);
                    GLFWImage.Buffer iconBuf = GLFWImage.malloc(1, stack);
                    icon.set(w, h, buffer);
                    iconBuf.put(0, icon);
                    GLFW.glfwSetWindowIcon(handle, iconBuf);
                }

                img.close();
                System.out.println("[VoxelClient] Fenster-Icon gesetzt (" + w + "x" + h + ")");

            } catch (Exception e) {
                System.err.println("[VoxelClient] Icon-Fehler: " + e.getMessage());
            }
        });
    }

    public void showDiscordRichPresence() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                // Singleplayer
                if (client.isInSingleplayer()) {
                    String worldName = client.getServer() != null
                            ? client.getServer().getSaveProperties().getLevelName()
                            : "Unbekannte Welt";
                    DiscordRPCManager.showSingleplayer(worldName);
                    System.out.println("[VoxelClient] ✔ Show singleplayer world with name " + worldName);
                    return;
                }

                // Multiplayer / Realms
                var serverEntry = client.getCurrentServerEntry();
                if (serverEntry != null) {
                    String address = serverEntry.address;
                    String name    = serverEntry.name;

                    if (address != null && address.endsWith(".realms.minecraft.net")) {
                        DiscordRPCManager.showRealms(name);
                        System.out.println("[VoxelClient] ✔ Show realm with name " + name);
                    } else {
                        DiscordRPCManager.showMultiplayer(name, address);
                        System.out.println("[VoxelClient] ✔ Show multiplayer screen with address " + address);
                    }
                }
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                DiscordRPCManager.showMainMenu()
        );
        System.out.println("[VoxelClient] ✔ Showing Discord Rich Presence!");
    }
}
