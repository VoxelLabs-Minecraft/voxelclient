package de.voxellabs.voxelclient.client.utils;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record HandshakePayload(UUID uuid) implements CustomPayload {

    public static final CustomPayload.Id<HandshakePayload> ID =
            new CustomPayload.Id<>(Identifier.of("voxelclient", "handshake"));

    public static final PacketCodec<RegistryByteBuf, HandshakePayload> CODEC =
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC, HandshakePayload::uuid,
                    HandshakePayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}