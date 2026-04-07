package rubbertoe.simple_atlas.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;

public final class AtlasContents {
    public static final AtlasContents EMPTY = new AtlasContents(List.of(), List.of(), 0, 1, 0);

    private static final int MAX_WAYPOINT_NAME_LENGTH = 32;

    public record WaypointData(double worldX, double worldZ, String name, int iconIndex) {
        public static final Codec<WaypointData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.fieldOf("world_x").forGetter(WaypointData::worldX),
                        Codec.DOUBLE.fieldOf("world_z").forGetter(WaypointData::worldZ),
                        Codec.STRING.fieldOf("name").forGetter(WaypointData::name),
                        Codec.INT.fieldOf("icon_index").forGetter(WaypointData::iconIndex)
                ).apply(instance, WaypointData::new)
        );

        public WaypointData {
            name = sanitizeName(name);
            iconIndex = Math.max(0, iconIndex);
        }
    }

    public static final Codec<AtlasContents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.listOf()
                            .optionalFieldOf("map_ids", List.of())
                            .forGetter(AtlasContents::mapIds),
                    WaypointData.CODEC.listOf()
                            .optionalFieldOf("waypoints", List.of())
                            .forGetter(AtlasContents::waypoints),
                    Codec.INT.optionalFieldOf("selected_waypoint_icon_index", 0)
                            .forGetter(AtlasContents::selectedWaypointIconIndex),
                    Codec.INT.optionalFieldOf("next_waypoint_number", 1)
                            .forGetter(AtlasContents::nextWaypointNumber),
                    Codec.INT.optionalFieldOf("blank_map_count", 0)
                            .forGetter(AtlasContents::blankMapCount)
            ).apply(instance, AtlasContents::new)
    );

    // LinkedHashSet: O(1) contains() + insertion-order iteration
    private final SequencedSet<Integer> mapIdSet;
    private final List<WaypointData> waypoints;
    private final int selectedWaypointIconIndex;
    private final int nextWaypointNumber;
    private final int blankMapCount;

    public AtlasContents(
            List<Integer> mapIds,
            List<WaypointData> waypoints,
            int selectedWaypointIconIndex,
            int nextWaypointNumber,
            int blankMapCount
    ) {
        this.mapIdSet = new LinkedHashSet<>(mapIds);
        this.waypoints = List.copyOf(waypoints);
        this.selectedWaypointIconIndex = Math.max(0, selectedWaypointIconIndex);
        this.nextWaypointNumber = Math.max(1, nextWaypointNumber);
        this.blankMapCount = Math.max(0, blankMapCount);
    }

    /** Returns map IDs in insertion order. */
    public List<Integer> mapIds() {
        return List.copyOf(mapIdSet);
    }

    public List<WaypointData> waypoints() {
        return waypoints;
    }

    public int selectedWaypointIconIndex() {
        return selectedWaypointIconIndex;
    }

    public int nextWaypointNumber() {
        return nextWaypointNumber;
    }

    public int blankMapCount() {
        return blankMapCount;
    }

    /** O(1) membership check. */
    public boolean contains(int mapId) {
        return mapIdSet.contains(mapId);
    }

    /** Returns a new {@code AtlasContents} with {@code mapId} appended, or {@code this} if already present. */
    public AtlasContents withAdded(int mapId) {
        if (contains(mapId)) {
            return this;
        }

        LinkedHashSet<Integer> updated = new LinkedHashSet<>(mapIdSet);
        updated.add(mapId);
        return new AtlasContents(List.copyOf(updated), waypoints, selectedWaypointIconIndex, nextWaypointNumber, blankMapCount);
    }

    public AtlasContents withWaypointState(List<WaypointData> waypoints, int selectedWaypointIconIndex, int nextWaypointNumber) {
        return new AtlasContents(mapIds(), waypoints, selectedWaypointIconIndex, nextWaypointNumber, blankMapCount);
    }

    public AtlasContents withAddedBlankMaps(int count) {
        if (count <= 0) {
            return this;
        }
        return new AtlasContents(mapIds(), waypoints, selectedWaypointIconIndex, nextWaypointNumber, blankMapCount + count);
    }

    public AtlasContents withConsumedBlankMap() {
        if (blankMapCount <= 0) {
            return this;
        }
        return new AtlasContents(mapIds(), waypoints, selectedWaypointIconIndex, nextWaypointNumber, blankMapCount - 1);
    }

    public int size() {
        return mapIdSet.size();
    }

    private static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }

        String trimmed = name.trim();
        if (trimmed.length() > MAX_WAYPOINT_NAME_LENGTH) {
            return trimmed.substring(0, MAX_WAYPOINT_NAME_LENGTH);
        }

        return trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasContents other)) return false;
        return mapIdSet.equals(other.mapIdSet)
                && waypoints.equals(other.waypoints)
                && selectedWaypointIconIndex == other.selectedWaypointIconIndex
                && nextWaypointNumber == other.nextWaypointNumber
                && blankMapCount == other.blankMapCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapIdSet, waypoints, selectedWaypointIconIndex, nextWaypointNumber, blankMapCount);
    }

    @Override
    public String toString() {
        return "AtlasContents{mapIds=" + mapIdSet + ", waypoints=" + waypoints.size() + ", blankMaps=" + blankMapCount + "}";
    }
}
