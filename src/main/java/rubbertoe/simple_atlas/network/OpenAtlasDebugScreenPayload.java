package rubbertoe.simple_atlas.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.SimpleAtlas;

import java.util.ArrayList;
import java.util.List;

public record OpenAtlasDebugScreenPayload(
        int width,
        int height,
        List<AtlasDebugTilePayload> tiles
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "open_atlas_debug");
    public static final Type<OpenAtlasDebugScreenPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, AtlasDebugTilePayload> TILE_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, AtlasDebugTilePayload::mapId,
                    ByteBufCodecs.INT, AtlasDebugTilePayload::centerX,
                    ByteBufCodecs.INT, AtlasDebugTilePayload::centerZ,
                    ByteBufCodecs.INT, AtlasDebugTilePayload::tileX,
                    ByteBufCodecs.INT, AtlasDebugTilePayload::tileY,
                    AtlasDebugTilePayload::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAtlasDebugScreenPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, OpenAtlasDebugScreenPayload::width,
                    ByteBufCodecs.INT, OpenAtlasDebugScreenPayload::height,
                    ByteBufCodecs.collection(ArrayList::new, TILE_CODEC), OpenAtlasDebugScreenPayload::tiles,
                    OpenAtlasDebugScreenPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}