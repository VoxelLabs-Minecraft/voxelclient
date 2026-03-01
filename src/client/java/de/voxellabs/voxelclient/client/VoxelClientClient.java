package de.voxellabs.voxelclient.client;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager;
import de.voxellabs.voxelclient.client.discord.DiscordRPCManager;
import de.voxellabs.voxelclient.client.features.FreelookFeature;
import de.voxellabs.voxelclient.client.features.ZoomFeature;
import de.voxellabs.voxelclient.client.gui.ClientModScreen;
import de.voxellabs.voxelclient.client.gui.CustomMainMenuScreen;
import de.voxellabs.voxelclient.client.gui.CustomServerListScreen;
import de.voxellabs.voxelclient.client.hud.HudRenderer;
import de.voxellabs.voxelclient.client.version.VersionChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class VoxelClientClient implements ClientModInitializer {

    public static final String MOD_VERSION = VersionChecker.CURRENT_VERSION;

    public static KeyBinding keyOpenMenu;
    public static KeyBinding keyZoom;
    public static KeyBinding keyFreeLook;

    @Override
    public void onInitializeClient() {
        System.out.println("[VoxelClient] ▶ Starting VoxelClient v" + MOD_VERSION + " (Minecraft 1.21.x / Fabric)");

        //1. Load configuration from disk
        VoxelClientConfig.load();

        //2. Update-Check asynchron
        VersionChecker.checkForUpdate();

        keyOpenMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voxelclient.openMenu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.voxelclient"
        ));

        keyZoom = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voxelclient.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "key.categories.voxelclient"
        ));

        keyFreeLook = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voxelclient.freelook",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "key.categories.voxelclient"
        ));

        HudRenderer.register();

        ZoomFeature.init(keyZoom);
        FreelookFeature.init(keyFreeLook);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            // TitleScreen → CustomMainMenuScreen
            if (client.currentScreen instanceof TitleScreen && client.world == null) {
                client.setScreen(new CustomMainMenuScreen());
                return;
            }

            // MultiplayerScreen → CustomServerListScreen
            if (client.currentScreen instanceof MultiplayerScreen) {
                client.setScreen(new CustomServerListScreen(null));
            }
        });

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

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                // Eigene UUID sofort vorladen
                if (client.player != null) {
                    UUID ownUuid = client.player.getUuid();
                    // Einmal aufrufen damit der async Fetch startet
                    BadgeApiClient.getBadge(ownUuid);
                }
            });
        });

        System.out.println("[VoxelClient] ✔ Initialisation complete!");
        System.out.println("[VoxelClient]   Pinned servers: Plantaria.net, ave.rip");
        System.out.println("[VoxelClient]   Update-Check läuft im Hintergrund…");
        System.out.println("[VoxelClient]   Press RIGHT SHIFT in-game to open settings.");

        showDiscordRichPresence();
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
