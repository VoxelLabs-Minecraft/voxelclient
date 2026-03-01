package de.voxellabs.voxelclient.client.utils;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record HandshakePayload(UUID uuid) implements CustomPayload {

    public static final CustomPayload.Id<HandshakePayload> ID =
            new CustomPayload.Id<>(Identifier.of("voxelclient", "handshake"));

    public static final PacketCodec<RegistryByteBuf, HandshakePayload> CODEC =
            new PacketCodec<>() {
                @Override
                public HandshakePayload decode(RegistryByteBuf buf) {
                    return new HandshakePayload(new UUID(buf.readLong(), buf.readLong()));
                }

                @Override
                public void encode(RegistryByteBuf buf, HandshakePayload value) {
                    buf.writeLong(value.uuid().getMostSignificantBits());
                    buf.writeLong(value.uuid().getLeastSignificantBits());
                }
            };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
