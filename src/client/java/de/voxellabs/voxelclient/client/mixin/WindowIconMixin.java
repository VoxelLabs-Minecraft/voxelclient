package de.voxellabs.voxelclient.client.mixin;

import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Mixin(Window.class)
public class WindowIconMixin {

    /**
     * Minecraft ruft setIcon() beim Start auf um das Standard-Icon zu setzen.
     * Wir canceln das und setzen stattdessen unser eigenes Icon.
     */
    @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
    private void overrideIcon(CallbackInfo ci) {
        ci.cancel();

        try (InputStream is = WindowIconMixin.class
                .getResourceAsStream("/icons/icon.png")) {

            if (is == null) {
                System.err.println("[VoxelClient] icon.png nicht gefunden!");
                return;
            }

            // BufferedImage lesen — kein Format-Problem mit NativeImage
            BufferedImage img = ImageIO.read(is);
            int w = img.getWidth();
            int h = img.getHeight();

            // Pixel in RGBA-ByteBuffer konvertieren
            ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    buffer.put((byte)((argb >> 16) & 0xFF)); // R
                    buffer.put((byte)((argb >>  8) & 0xFF)); // G
                    buffer.put((byte)( argb        & 0xFF)); // B
                    buffer.put((byte)((argb >> 24) & 0xFF)); // A
                }
            }
            buffer.flip();

            long handle = ((Window)(Object)this).getHandle();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                GLFWImage.Buffer icons = GLFWImage.malloc(1, stack);
                icons.position(0).width(w).height(h).pixels(buffer);
                GLFW.glfwSetWindowIcon(handle, icons);
            }

            System.out.println("[VoxelClient] Icon gesetzt: " + w + "x" + h);

        } catch (Exception e) {
            System.err.println("[VoxelClient] Icon-Fehler: " + e.getMessage());
        }
    }
}
