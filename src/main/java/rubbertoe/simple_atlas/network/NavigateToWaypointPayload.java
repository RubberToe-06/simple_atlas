package rubbertoe.simple_atlas.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.SimpleAtlas;

public record NavigateToWaypointPayload(
        double worldX,
        double worldZ,
        String waypointName
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "navigate_to_waypoint");
    public static final Type<NavigateToWaypointPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, NavigateToWaypointPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    NavigateToWaypointPayload::worldX,
                    ByteBufCodecs.DOUBLE,
                    NavigateToWaypointPayload::worldZ,
                    ByteBufCodecs.stringUtf8(32),
                    NavigateToWaypointPayload::waypointName,
                    NavigateToWaypointPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

