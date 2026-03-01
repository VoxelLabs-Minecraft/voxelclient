package de.voxellabs.voxelclient.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.voxellabs.voxelclient.client.badge.BadgeRenderer;
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

        // Badge-String holen (§4✦ §r für Creatoren, §7✦ §r für alle)
        String badge = BadgeRenderer.getBadgeString(uuid);

        // Badge vor den originalen Namen stellen
        MutableText badgeText = Text.literal(badge);
        return badgeText.append(original);
    }
}
