package rubbertoe.simple_atlas.debug;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.component.AtlasContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AtlasLayoutDebugger {
    private AtlasLayoutDebugger() {}

    public static AtlasDebugLayout build(ServerLevel level, AtlasContents contents) {
        if (contents.mapIds().isEmpty()) {
            return emptyLayout();
        }

        List<ResolvedMap> resolved = new ArrayList<>();

        for (int rawId : contents.mapIds()) {
            MapId mapId = new MapId(rawId);
            MapItemSavedData data = level.getMapData(mapId);

            if (data == null) {
                continue;
            }

            resolved.add(new ResolvedMap(
                    rawId,
                    data.centerX,
                    data.centerZ,
                    data.scale
            ));
        }

        if (resolved.isEmpty()) {
            return emptyLayout();
        }

        resolved.sort(Comparator.comparingInt(ResolvedMap::mapId));
        ResolvedMap origin = resolved.getFirst();

        int originScale = origin.scale();
        int mapSpan = 128 << originScale;

        List<RawEntry> rawEntries = new ArrayList<>();
        int minGridX = Integer.MAX_VALUE;
        int maxGridX = Integer.MIN_VALUE;
        int minGridZ = Integer.MAX_VALUE;
        int maxGridZ = Integer.MIN_VALUE;

        for (ResolvedMap map : resolved) {
            if (map.scale() != originScale) {
                continue;
            }

            int dx = map.centerX() - origin.centerX();
            int dz = map.centerZ() - origin.centerZ();

            int gridX = dx / mapSpan;
            int gridZ = dz / mapSpan;

            rawEntries.add(new RawEntry(
                    map.mapId(),
                    map.centerX(),
                    map.centerZ(),
                    map.scale(),
                    mapSpan,
                    gridX,
                    gridZ
            ));

            if (gridX < minGridX) minGridX = gridX;
            if (gridX > maxGridX) maxGridX = gridX;
            if (gridZ < minGridZ) minGridZ = gridZ;
            if (gridZ > maxGridZ) maxGridZ = gridZ;
        }

        if (rawEntries.isEmpty()) {
            return emptyLayout();
        }

        int width = maxGridX - minGridX + 1;
        int height = maxGridZ - minGridZ + 1;

        List<AtlasMapDebugEntry> entries = getAtlasMapDebugEntries(rawEntries, minGridX, minGridZ);

        entries.sort(Comparator
                .comparingInt(AtlasMapDebugEntry::tileY)
                .thenComparingInt(AtlasMapDebugEntry::tileX)
                .thenComparingInt(AtlasMapDebugEntry::mapId));

        return new AtlasDebugLayout(
                entries,
                origin.mapId(),
                origin.centerX(),
                origin.centerZ(),
                origin.scale(),
                mapSpan,
                minGridX,
                maxGridX,
                minGridZ,
                maxGridZ,
                width,
                height
        );
    }

    private static @NonNull List<AtlasMapDebugEntry> getAtlasMapDebugEntries(List<RawEntry> rawEntries, int minGridX, int minGridZ) {
        List<AtlasMapDebugEntry> entries = new ArrayList<>();
        for (RawEntry raw : rawEntries) {
            int tileX = raw.gridX() - minGridX;
            int tileY = raw.gridZ() - minGridZ;

            entries.add(new AtlasMapDebugEntry(
                    raw.mapId(),
                    raw.centerX(),
                    raw.centerZ(),
                    raw.scale(),
                    raw.mapSpan(),
                    raw.gridX(),
                    raw.gridZ(),
                    tileX,
                    tileY
            ));
        }
        return entries;
    }

    private static AtlasDebugLayout emptyLayout() {
        return new AtlasDebugLayout(
                List.of(),
                -1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }

    private record ResolvedMap(
            int mapId,
            int centerX,
            int centerZ,
            int scale
    ) {}

    private record RawEntry(
            int mapId,
            int centerX,
            int centerZ,
            int scale,
            int mapSpan,
            int gridX,
            int gridZ
    ) {}
}