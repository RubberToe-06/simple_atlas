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

public record SaveAtlasWaypointsPayload(
        List<Integer> atlasMapIds,
        List<AtlasContents.WaypointData> waypoints,
        int selectedWaypointIconIndex,
        int nextWaypointNumber
) implements CustomPacketPayload {
    public static final Type<SaveAtlasWaypointsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "save_atlas_waypoints"));

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

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveAtlasWaypointsPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT, AtlasContents.MAX_ATLAS_MAP_COUNT),
                    p -> new ArrayList<>(p.atlasMapIds()),
                    ByteBufCodecs.collection(ArrayList::new, WAYPOINT_CODEC, ModNetworking.MAX_WAYPOINT_COUNT),
                    p -> new ArrayList<>(p.waypoints()),
                    ByteBufCodecs.INT,
                    SaveAtlasWaypointsPayload::selectedWaypointIconIndex,
                    ByteBufCodecs.INT,
                    SaveAtlasWaypointsPayload::nextWaypointNumber,
                    SaveAtlasWaypointsPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

