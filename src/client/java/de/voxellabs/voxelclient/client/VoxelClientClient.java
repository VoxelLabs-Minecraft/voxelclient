package de.voxellabs.voxelclient.client;

import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsApiClient;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsCatalogClient;
import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
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
import de.voxellabs.voxelclient.client.utils.HandshakePayload;
import de.voxellabs.voxelclient.client.utils.VoxelClientNetwork;
import de.voxellabs.voxelclient.client.version.VersionChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

        // 1. Konfiguration laden
        VoxelClientConfig.load();

        // 2. Update-Check asynchron
        VersionChecker.checkForUpdate();

        // 3. Discord RPC
        DiscordRPCManager.init();

        // 4. Cosmetics-Katalog vorladen (global, einmalig, kein Login nötig)
        //    Wird von TrailRenderer + CosmeticsFeatureRenderer benötigt um
        //    Item-IDs in URLs/trail_ids aufzulösen.
        CosmeticsCatalogClient.fetch(catalog -> {
            if (catalog != null && !catalog.isEmpty()) {
                System.out.println("[VoxelClient] ✔ Cosmetics-Katalog geladen ("
                        + catalog.types.size() + " Typen)");
            } else {
                System.err.println("[VoxelClient] ⚠ Cosmetics-Katalog ist leer oder konnte nicht geladen werden");
            }
        });

        // Key Bindings
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

        // HUD
        DraggableHudSystem.register();
        FPSHud.register();
        DirectionHud.register();
        SpeedHud.register();
        ArmorDurabilityHud.register();
        KeystrokesHud.register();
        CpsCounter.register();
        PingHud.register();

        // Gameplay
        ToggleSprintModule.register();
        ToggleSneakModule.register();
        SnapLookModule.register();

        // Utility
        ZoomFeature.init(keyZoom);
        FreelookFeature.init(keyFreeLook);

        // Cosmetics (registriert auch TrailRenderer via CosmeticsFeatureRenderer.register())
        CosmeticsFeatureRenderer.register();

        // Networking
        VoxelClientNetwork.init();

        // Listeners
        initLifeCycleListeners();
        initClientTickListeners();
        initClientConnectionListeners();

        System.out.println("[VoxelClient] ✔ Initialisation complete!");
        System.out.println("[VoxelClient]   Pinned servers: Plantaria.net, ave.rip");
        System.out.println("[VoxelClient]   Update-Check läuft im Hintergrund…");
        System.out.println("[VoxelClient]   Press RIGHT SHIFT in-game to open settings.");

        showDiscordRichPresence();
    }

    public void initLifeCycleListeners() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            DiscordRPCManager.showMainMenu();
        });
    }

    public void initClientTickListeners() {
        // Cosmetics + Badge vorladen — nur für den eigenen Spieler beim ENTITY_LOAD.
        // Andere Spieler werden lazy geladen wenn sie tatsächlich gerendert werden
        // (getCosmetics() ruft prefetch() intern auf). Das verhindert Massen-Requests
        // auf großen Servern.
        // ENTITY_LOAD: nur eigenen Spieler laden.
        // Andere Spieler werden via VoxelClient-Handshake (VoxelClientNetwork) erkannt
        // und dann gebatcht geladen — kein Einzel-Request pro sichtbarem Spieler mehr.
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof AbstractClientPlayerEntity player) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null && mc.player.getUuid().equals(player.getUuid())) {
                    CosmeticsApiClient.prefetch(player.getUuid());
                    BadgeApiClient.prefetch(player.getUuid());
                }
            }
        });

        // Beim Disconnect: Spieler-spezifische Caches leeren.
        // CosmeticsCatalogClient wird NICHT geleert – der Katalog ist
        // server-unabhängig und bleibt für den nächsten Join gültig.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CosmeticsApiClient.clearCache();
            BadgeApiClient.clearCache();
        });

        // RShift → Menü öffnen + sichtbare Spieler vorladen
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyOpenMenu.wasPressed()) {
                // Kein loadAllVisible mehr — verursacht Massen-Requests auf großen Servern.
                // Der Screen lädt den eigenen Spieler über fetchWithCallback direkt.
                client.setScreen(new ClientModScreen());
            }
        });

        // Fenstertitel mit Branding
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private boolean lastFullscreen = false;
            private boolean initialized    = false;

            @Override
            public void onEndTick(MinecraftClient client) {
                boolean fullscreen = client.getWindow().isFullscreen();
                if (!initialized || fullscreen != lastFullscreen) {
                    initialized    = true;
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
                if (client.player != null) {
                    UUID ownUuid = client.player.getUuid();
                    // Spieler-Cosmetics neu laden (Server kann unterschiedliche Items haben)
                    CosmeticsApiClient.clearCache();
                    CosmeticsApiClient.prefetch(ownUuid);

                    // Katalog nachladen falls er beim Start noch nicht fertig war
                    if (!CosmeticsCatalogClient.isLoaded()) {
                        CosmeticsCatalogClient.fetch(catalog ->
                                System.out.println("[VoxelClient] ✔ Katalog nachgeladen beim Join")
                        );
                    }
                }
            });
        });
    }

    public void showDiscordRichPresence() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                if (client.isInSingleplayer()) {
                    String worldName = client.getServer() != null
                            ? client.getServer().getSaveProperties().getLevelName()
                            : "Unbekannte Welt";
                    DiscordRPCManager.showSingleplayer(worldName);
                    System.out.println("[VoxelClient] ✔ Show singleplayer world with name " + worldName);
                    return;
                }

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