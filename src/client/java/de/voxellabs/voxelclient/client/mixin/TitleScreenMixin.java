package de.voxellabs.voxelclient.client.mixin;

import de.voxellabs.voxelclient.client.gui.CustomMainMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla Title Screen with MyClient's custom main menu.
 *
 * Strategy: intercept {@code TitleScreen#init()} and immediately swap
 * the active screen to {@link CustomMainMenuScreen}.
 * This fires on every occasion Minecraft would show the title screen
 * (startup, disconnect, quit world, etc.).
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init()V", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        // Only swap if we are actually going to show the TitleScreen
        // (not during resource reload init calls where currentScreen != this)
        if (mc.currentScreen instanceof TitleScreen) {
            mc.setScreen(new CustomMainMenuScreen());
            ci.cancel();
        }
    }
}
