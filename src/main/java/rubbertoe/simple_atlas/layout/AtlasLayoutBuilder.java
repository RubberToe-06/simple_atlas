package rubbertoe.simple_atlas.layout;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import rubbertoe.simple_atlas.component.AtlasContents;

import java.util.*;

public final class AtlasLayoutBuilder {
    private AtlasLayoutBuilder() {}

    public static AtlasLayout build(ServerLevel level, AtlasContents contents) {
        if (contents.mapIds().isEmpty()) {
            return emptyLayout();
        }

        List<ResolvedMap> resolved = new ArrayList<>();
        for (int rawId : contents.mapIds()) {
            MapId mapId = new MapId(rawId);
            MapItemSavedData data = level.getMapData(mapId);
            if (data == null) continue;
            resolved.add(new ResolvedMap(
                    rawId,
                    data.centerX,
                    data.centerZ,
                    data.scale,
                    data.dimension.identifier().toString()
            ));
        }

        if (resolved.isEmpty()) {
            return emptyLayout();
        }

        Map<String, List<ResolvedMap>> byDimension = new LinkedHashMap<>();
        for (ResolvedMap map : resolved) {
            byDimension.computeIfAbsent(map.dimension(), _ -> new ArrayList<>()).add(map);
        }

        ResolvedMap globalOrigin = resolved.getFirst();
        int originScale = globalOrigin.scale();
        int mapSpan = 128 << originScale;

        List<RawEntry> allEntries = new ArrayList<>();
        int globalMinGridX = Integer.MAX_VALUE;
        int globalMaxGridX = Integer.MIN_VALUE;
        int globalMinGridZ = Integer.MAX_VALUE;
        int globalMaxGridZ = Integer.MIN_VALUE;

        Map<String, int[]> dimensionBounds = new LinkedHashMap<>();

        for (Map.Entry<String, List<ResolvedMap>> dimEntry : byDimension.entrySet()) {
            String dimension = dimEntry.getKey();
            List<ResolvedMap> dimMaps = dimEntry.getValue();

            ResolvedMap dimOrigin = dimMaps.getFirst();
            int dimMapSpan = 128 << dimOrigin.scale();

            int dimMinGridX = Integer.MAX_VALUE;
            int dimMaxGridX = Integer.MIN_VALUE;
            int dimMinGridZ = Integer.MAX_VALUE;
            int dimMaxGridZ = Integer.MIN_VALUE;

            List<RawEntry> dimEntries = new ArrayList<>();
            for (ResolvedMap map : dimMaps) {
                if (map.scale() != dimOrigin.scale()) continue;

                int dx = map.centerX() - dimOrigin.centerX();
                int dz = map.centerZ() - dimOrigin.centerZ();
                int gridX = dx / dimMapSpan;
                int gridZ = dz / dimMapSpan;

                dimEntries.add(new RawEntry(
                        map.mapId(), map.centerX(), map.centerZ(),
                        map.scale(), dimMapSpan, gridX, gridZ, dimension
                ));

                dimMinGridX = Math.min(dimMinGridX, gridX);
                dimMaxGridX = Math.max(dimMaxGridX, gridX);
                dimMinGridZ = Math.min(dimMinGridZ, gridZ);
                dimMaxGridZ = Math.max(dimMaxGridZ, gridZ);
            }

            if (dimEntries.isEmpty()) continue;

            dimensionBounds.put(dimension, new int[]{dimMinGridX, dimMaxGridX, dimMinGridZ, dimMaxGridZ});
            allEntries.addAll(dimEntries);

            globalMinGridX = Math.min(globalMinGridX, dimMinGridX);
            globalMaxGridX = Math.max(globalMaxGridX, dimMaxGridX);
            globalMinGridZ = Math.min(globalMinGridZ, dimMinGridZ);
            globalMaxGridZ = Math.max(globalMaxGridZ, dimMaxGridZ);
        }

        if (allEntries.isEmpty()) {
            return emptyLayout();
        }

        List<AtlasMapEntry> entries = new ArrayList<>();
        for (RawEntry raw : allEntries) {
            int[] bounds = dimensionBounds.get(raw.dimension());
            int localMinGridX = bounds[0];
            int localMinGridZ = bounds[2];
            int tileX = raw.gridX() - localMinGridX;
            int tileY = raw.gridZ() - localMinGridZ;

            entries.add(new AtlasMapEntry(
                    raw.mapId(), raw.centerX(), raw.centerZ(),
                    raw.scale(), raw.mapSpan(), raw.gridX(), raw.gridZ(),
                    tileX, tileY
            ));
        }

        entries.sort(Comparator
                .comparingInt(AtlasMapEntry::tileY)
                .thenComparingInt(AtlasMapEntry::tileX)
                .thenComparingInt(AtlasMapEntry::mapId));

        int width = globalMaxGridX - globalMinGridX + 1;
        int height = globalMaxGridZ - globalMinGridZ + 1;

        return new AtlasLayout(
                entries,
                globalOrigin.mapId(),
                globalOrigin.centerX(),
                globalOrigin.centerZ(),
                originScale,
                mapSpan,
                globalMinGridX,
                globalMaxGridX,
                globalMinGridZ,
                globalMaxGridZ,
                width,
                height
        );
    }

    private static AtlasLayout emptyLayout() {
        return new AtlasLayout(List.of(), -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private record ResolvedMap(int mapId, int centerX, int centerZ, int scale, String dimension) {}

    private record RawEntry(int mapId, int centerX, int centerZ, int scale, int mapSpan,
                            int gridX, int gridZ, String dimension) {}
}
