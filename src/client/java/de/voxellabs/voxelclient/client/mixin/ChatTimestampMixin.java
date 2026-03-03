package de.voxellabs.voxelclient.client.mixin;

import de.voxellabs.voxelclient.client.config.ChatTimestampConfig;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Chat Zeitstempel Mixin — VoxelClient v0.0.2
 * Fügt automatisch einen Zeitstempel vor jede Chat-Nachricht ein.
 * Format: [HH:mm] Nachricht
 */
@Mixin(ChatHud.class)
public class ChatTimestampMixin {

    @Unique
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text injectTimestamp(Text message) {
        if (!ChatTimestampConfig.enabled) return message;

        String time = LocalTime.now().format(FORMATTER);

        MutableText timestamp = Text.literal("[" + time + "] ")
                .setStyle(Style.EMPTY.withColor(0x888888));

        return timestamp.append(message);
    }
}
