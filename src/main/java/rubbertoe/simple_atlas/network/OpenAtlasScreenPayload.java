package rubbertoe.simple_atlas.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.SimpleAtlas;
import rubbertoe.simple_atlas.component.AtlasContents;

import java.util.ArrayList;
import java.util.List;

public record OpenAtlasScreenPayload(
        List<AtlasTilePayload> tiles,
        List<Integer> atlasMapIds,
        List<AtlasContents.WaypointData> waypoints,
        int selectedWaypointIconIndex,
        int nextWaypointNumber,
        String playerDimension
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "open_atlas_screen");
    public static final Type<OpenAtlasScreenPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, AtlasTilePayload> TILE_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, AtlasTilePayload::mapId,
                    ByteBufCodecs.INT, AtlasTilePayload::centerX,
                    ByteBufCodecs.INT, AtlasTilePayload::centerZ,
                    ByteBufCodecs.INT, AtlasTilePayload::tileX,
                    ByteBufCodecs.INT, AtlasTilePayload::tileY,
                    ByteBufCodecs.stringUtf8(256), AtlasTilePayload::dimension,
                    AtlasTilePayload::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, AtlasContents.WaypointData> WAYPOINT_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    AtlasContents.WaypointData::worldX,
                    ByteBufCodecs.DOUBLE,
                    AtlasContents.WaypointData::worldZ,
                    ByteBufCodecs.stringUtf8(32),
                    AtlasContents.WaypointData::name,
                    ByteBufCodecs.INT,
                    AtlasContents.WaypointData::iconIndex,
                    ByteBufCodecs.stringUtf8(256),
                    AtlasContents.WaypointData::dimension,
                    AtlasContents.WaypointData::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAtlasScreenPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, TILE_CODEC),
                    p -> new ArrayList<>(p.tiles()),
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT),
                    p -> new ArrayList<>(p.atlasMapIds()),
                    ByteBufCodecs.collection(ArrayList::new, WAYPOINT_CODEC),
                    p -> new ArrayList<>(p.waypoints()),
                    ByteBufCodecs.INT,
                    OpenAtlasScreenPayload::selectedWaypointIconIndex,
                    ByteBufCodecs.INT,
                    OpenAtlasScreenPayload::nextWaypointNumber,
                    ByteBufCodecs.stringUtf8(256),
                    OpenAtlasScreenPayload::playerDimension,
                    OpenAtlasScreenPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

