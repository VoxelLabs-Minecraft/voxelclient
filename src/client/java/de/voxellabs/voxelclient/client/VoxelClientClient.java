package de.voxellabs.voxelclient.client;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager;
import de.voxellabs.voxelclient.client.cosmetics.renderer.CosmeticsFeatureRenderer;
import de.voxellabs.voxelclient.client.discord.DiscordRPCManager;
import de.voxellabs.voxelclient.client.ui.module.gameplay.SnapLookModule;
import de.voxellabs.voxelclient.client.ui.module.gameplay.ToggleSneakModule;
import de.voxellabs.voxelclient.client.ui.module.gameplay.ToggleSprintModule;
import de.voxellabs.voxelclient.client.ui.module.hud.*;
import de.voxellabs.voxelclient.client.ui.module.utility.FreelookFeature;
import de.voxellabs.voxelclient.client.ui.module.utility.ZoomFeature;
import de.voxellabs.voxelclient.client.ui.gui.ClientModScreen;
import de.voxellabs.voxelclient.client.ui.hud.DraggableHudSystem;
import de.voxellabs.voxelclient.client.utils.VoxelClientNetwork;
import de.voxellabs.voxelclient.client.version.VersionChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
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

        //2.5 Init discordrpc
        DiscordRPCManager.init();

        keyOpenMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "voxelclient.key.openMenu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "voxelclient.key.category"
        ));

        keyZoom = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "voxelclient.key.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "voxelclient.key.category"
        ));

        keyFreeLook = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "voxelclient.key.freelook",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "voxelclient.key.category"
        ));
        // Hud renderer
        DraggableHudSystem.register();

        // GamePlay
        ToggleSprintModule.register();
        ToggleSneakModule.register();
        SnapLookModule.register();

        // Utility
        ZoomFeature.init(keyZoom);
        FreelookFeature.init(keyFreeLook);
        KeystrokesHud.register();
        CpsCounter.register();
        ArmorDurabilityHud.register();
        PingHud.register();
        FPSHud.register();
        SpeedHud.register();
        DirectionHud.register();

        // Cosmetics
        CosmeticsFeatureRenderer.register();

        // Load Networking methods for handshake
        VoxelClientNetwork.init();

        // Init listeners
        initLifeCylceListeners();
        initClientTickListeners();
        initClientConnectionListeners();

        // Logs
        System.out.println("[VoxelClient] ✔ Initialisation complete!");
        System.out.println("[VoxelClient]   Pinned servers: Plantaria.net, ave.rip");
        System.out.println("[VoxelClient]   Update-Check läuft im Hintergrund…");
        System.out.println("[VoxelClient]   Press RIGHT SHIFT in-game to open settings.");

        // Discord-RPC
        showDiscordRichPresence();
    }

    public void initLifeCylceListeners() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            DiscordRPCManager.showMainMenu();
        });
    }

    public void initClientTickListeners() {
        ClientEntityEvents.ENTITY_LOAD.register((client, entity) -> {
            if (client instanceof AbstractClientPlayerEntity player) {
                CosmeticsApiClient.prefetch(player.getUuid());
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((client, disconnect) -> {
            CosmeticsApiClient.clearCache();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyOpenMenu.wasPressed()) {
                CosmeticsApiClient.loadAllVisible(MinecraftClient.getInstance());
                client.setScreen(new ClientModScreen());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private int delay = 20; // wait 1 second (20 ticks)
            private boolean done = false;

            @Override
            public void onEndTick(MinecraftClient client) {
                if (!done && client.player != null && delay-- <= 0) {
                    CosmeticsManager.loadCosmetics(client.player.getUuid());
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
    }

    public void initClientConnectionListeners() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                // Eigene UUID sofort vorladen
                if (client.player != null) {
                    UUID ownUuid = client.player.getUuid();
                    // Einmal aufrufen damit der async Fetch startet
                    CosmeticsApiClient.clearCache();
                    BadgeApiClient.getBadge(ownUuid);
                    CosmeticsApiClient.prefetch(ownUuid);
                }
            });
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
