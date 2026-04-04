package rubbertoe.simple_atlas.client.waypoint;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AtlasWaypointStore {
    private static final Map<String, WaypointState> STATE_BY_ATLAS = new HashMap<>();

    private AtlasWaypointStore() {}

    public static WaypointState getOrCreate(List<AtlasTilePayload> tiles) {
        String key = createAtlasKey(tiles);
        return STATE_BY_ATLAS.computeIfAbsent(key, ignored -> new WaypointState());
    }

    private static String createAtlasKey(List<AtlasTilePayload> tiles) {
        return tiles.stream()
                .map(tile -> Integer.toString(tile.mapId()))
                .collect(Collectors.joining(","));
    }

    public static final class WaypointState {
        public final List<WaypointData> waypoints = new ArrayList<>();
        public int selectedWaypointIconIndex = 0;
        public int nextWaypointNumber = 1;
    }

    public record WaypointData(double worldX, double worldZ, String name, int iconIndex) {}
}

