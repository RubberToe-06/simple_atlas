package rubbertoe.simple_atlas.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.SimpleAtlas;
import rubbertoe.simple_atlas.component.AtlasContents;

import java.util.ArrayList;
import java.util.List;

public record RemoveAtlasMapPayload(
        List<Integer> atlasMapIds,
        int mapId
) implements CustomPacketPayload {
    public static final Type<RemoveAtlasMapPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "remove_atlas_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveAtlasMapPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT, AtlasContents.MAX_ATLAS_MAP_COUNT),
                    payload -> new ArrayList<>(payload.atlasMapIds()),
                    ByteBufCodecs.INT,
                    RemoveAtlasMapPayload::mapId,
                    RemoveAtlasMapPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

