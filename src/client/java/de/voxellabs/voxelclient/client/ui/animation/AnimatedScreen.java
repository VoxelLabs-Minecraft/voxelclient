package de.voxellabs.voxelclient.client.ui.animation;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * AnimatedScreen — VoxelClient v0.0.2
 * Basis-Klasse für alle VoxelClient-Screens mit integrierter Einblend-Animation.
 *
 * Verwendung:
 *   public class MyScreen extends AnimatedScreen {
 *       public MyScreen() { super(Text.literal("Titel")); }
 *       ...
 *   }
 */
public abstract class AnimatedScreen extends Screen {

    protected final UiAnimation animation;
    private static final int SLIDE_AMOUNT = 20;

    public AnimatedScreen(Text title) {
        super(title);
        this.animation = UiAnimation.screenFade();
    }

    @Override
    protected void init() {
        animation.start();
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float progress = animation.getProgress();
        int yOffset = animation.getYOffset(SLIDE_AMOUNT);

        context.getMatrices().push();
        context.getMatrices().translate(0, yOffset, 0);

        renderContent(context, mouseX, mouseY - yOffset, delta, progress);

        context.getMatrices().pop();
    }

    // Diese Methode in Subklassen überschreiben — NICHT abstract!
    protected void renderContent(DrawContext context, int mouseX, int mouseY,
                                 float delta, float progress) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}
