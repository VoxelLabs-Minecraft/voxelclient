package de.voxellabs.voxelclient.client.utils;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class RenderStateUuidCache {

    private static final Map<Object, UUID> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RenderStateUuidCache() {}

    public static void put(Object renderState, UUID uuid) {
        CACHE.put(renderState, uuid);
    }

    public static UUID get(Object renderState) {
        return CACHE.get(renderState);
    }
}
