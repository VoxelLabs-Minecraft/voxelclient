package de.voxellabs.voxelclient.client.mixin.ui;

import de.voxellabs.voxelclient.client.ui.gui.CustomServerListScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MutliplayerScreenMixin {

    @Unique
    private boolean replaced = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!replaced) {
            replaced = true;
            MinecraftClient.getInstance().setScreen(new CustomServerListScreen(null));
        }
    }

}
