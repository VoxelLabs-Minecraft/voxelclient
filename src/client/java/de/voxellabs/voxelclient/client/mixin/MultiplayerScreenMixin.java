package de.voxellabs.voxelclient.client.mixin;

import de.voxellabs.voxelclient.client.gui.CustomServerListScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects the vanilla MultiplayerScreen to our CustomServerListScreen.
 *
 * This catches any code path that tries to open the multiplayer screen
 * directly (e.g. command-line flags, mods, or the "Join Server" button
 * on the pause menu), ensuring our pinned-server list is always shown.
 */
@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {

    @Inject(method = "init()V", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof MultiplayerScreen multiplayerScreen) {
            // Replace with our custom screen, passing the MultiplayerScreen's parent
            mc.setScreen(new CustomServerListScreen(mc.currentScreen));
            ci.cancel();
        }
    }
}
