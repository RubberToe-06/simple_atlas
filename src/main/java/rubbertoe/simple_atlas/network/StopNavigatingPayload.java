package rubbertoe.simple_atlas.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.SimpleAtlas;

public record StopNavigatingPayload() implements CustomPacketPayload {
    public static final Type<StopNavigatingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "stop_navigating"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StopNavigatingPayload> CODEC =
            StreamCodec.unit(new StopNavigatingPayload());

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

