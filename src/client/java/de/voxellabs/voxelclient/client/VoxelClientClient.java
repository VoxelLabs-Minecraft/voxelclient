package de.voxellabs.voxelclient.client;

import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.cosmetics.CosmeticsManager;
import de.voxellabs.voxelclient.client.features.FreelookFeature;
import de.voxellabs.voxelclient.client.features.ZoomFeature;
import de.voxellabs.voxelclient.client.gui.ClientModScreen;
import de.voxellabs.voxelclient.client.hud.HudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VoxelClientClient implements ClientModInitializer {

    public static final String MOD_ID = "voxelclient";

    public static KeyBinding keyOpenMenu;
    public static KeyBinding keyZoom;
    public static KeyBinding keyFreeLook;

    @Override
    public void onInitializeClient() {
        System.out.println("[MyClient] Initialising MyClient for Minecraft 1.21.x (Fabric)");
        VoxelClientConfig.load();

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
        System.out.println("[MyClient] Initialisation complete! Press RIGHT SHIFT to open settings.");
    }
}
