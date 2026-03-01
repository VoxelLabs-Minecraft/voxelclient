package de.voxellabs.voxelclient.client.mixin;

import de.voxellabs.voxelclient.client.version.VersionChecker;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowTitleMixin {

    @Unique
    private static final String TITLE = "VoxelClient v" + VersionChecker.CURRENT_VERSION;

    /**
     * Überschreibt den Fenstertitel bei jedem Versuch ihn zu setzen.
     * So bleibt er permanent auf unserem Titel — egal was Minecraft versucht zu setzen.
     */
    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void onSetTitle(String title, CallbackInfo ci) {
        // Original-Aufruf abbrechen und unseren Titel setzen
        org.lwjgl.glfw.GLFW.glfwSetWindowTitle(
                ((Window)(Object)this).getHandle(),
                TITLE
        );
        ci.cancel();
    }
}
