package de.voxellabs.voxelclient.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.voxellabs.voxelclient.client.badge.BadgeApiClient;
import de.voxellabs.voxelclient.client.badge.BadgeRenderer;
import de.voxellabs.voxelclient.client.utils.VoxelClientNetwork;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

/**
 * Fügt das VoxelClient-Badge in die Tab-Liste (F1-Spielerliste) ein.
 *
 * Strategie: Den Spielernamen-Text abfangen und das Badge-Zeichen voranstellen.
 * Das ist stabiler als in die Render-Methode einzugreifen.
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    /**
     * Modifiziert den angezeigten Spielernamen in der Tab-Liste.
     * Das Badge-Zeichen wird dem Text vorangestellt.
     *
     * In 1.21.4: getPlayerName(PlayerListEntry) → Text
     */
    @ModifyReturnValue(
            method = "getPlayerName",
            at = @At("RETURN")
    )
    private Text injectBadge(Text original, PlayerListEntry entry) {
        UUID uuid = entry.getProfile().getId();
        if (!VoxelClientNetwork.isVoxelUser(uuid)) return original;

        BadgeApiClient.CachedBadge badge = BadgeApiClient.getBadge(uuid);
        if (badge == null) return original;

        String prefix = BadgeApiClient.getBadgeString(uuid);
        MutableText badgeText = Text.literal(prefix);
        return badgeText.append(original);
    }
}
