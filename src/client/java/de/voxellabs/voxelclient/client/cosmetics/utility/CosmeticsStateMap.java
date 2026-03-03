package de.voxellabs.voxelclient.client.cosmetics.utility;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class CosmeticsStateMap {

    private static final Map<PlayerEntityRenderState, UUID> MAP = new WeakHashMap<>();

    public static void put(PlayerEntityRenderState state, UUID uuid) {
        MAP.put(state, uuid);
    }

    public static UUID get(PlayerEntityRenderState state) {
        return MAP.get(state);
    }
}
