package de.voxellabs.voxelclient.client.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.config.VoxelClientConfig;
import de.voxellabs.voxelclient.client.utils.VoxelClientNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text injectBadge(Text original, PlayerListEntry entry) {
        UUID uuid = entry.getProfile().getId();
        if (!VoxelClientNetwork.isVoxelUser(uuid)) return original;

        String prefix = resolveBadgePrefix(uuid);
        if (prefix == null) return original;

        return Text.literal(prefix).append(original);
    }

    private static String resolveBadgePrefix(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isOwnPlayer = mc.player != null && mc.player.getUuid().equals(uuid);

        if (isOwnPlayer) {
            int activeId = VoxelClientConfig.get().activeBadgeId;
            if (activeId == 0) return null; // Spieler hat kein Badge gewählt
            // Badge aus dem Katalog laden (nach ID) statt Server-Antwort verwenden
            BadgeApiClient.CachedBadge badge = BadgeApiClient.getBadgeById(activeId);
            if (badge == null) return null;
            return formatBadgePrefix(badge);
        }

        // Andere Spieler: Server-Antwort verwenden
        BadgeApiClient.CachedBadge badge = BadgeApiClient.getBadge(uuid);
        if (badge == null) return null;
        return formatBadgePrefix(badge);
    }

    private static String formatBadgePrefix(BadgeApiClient.CachedBadge badge) {
        return BadgeApiClient.formatColor(badge.color)
                + (badge.icon != null ? badge.icon : "✦") + " §r";
    }
}