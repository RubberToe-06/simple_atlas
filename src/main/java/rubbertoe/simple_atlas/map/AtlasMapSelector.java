package rubbertoe.simple_atlas.map;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class AtlasMapSelector {
    private AtlasMapSelector() {}

    public static @Nullable Integer findCurrentMapRawId(
            Level level,
            double x,
            double z,
            List<Integer> mapIds,
            @Nullable Integer preferredRawId
    ) {
        if (preferredRawId != null && mapIds.contains(preferredRawId) && mapCoversPosition(level, x, z, preferredRawId)) {
            return preferredRawId;
        }

        Integer bestMapId = null;
        int bestScaleFactor = Integer.MAX_VALUE;
        double bestCenterDistanceSq = Double.MAX_VALUE;

        for (int rawId : mapIds) {
            MapItemSavedData mapData = level.getMapData(new MapId(rawId));
            if (!mapContainsPosition(level, x, z, mapData)) {
                continue;
            }

            // Prefer the most detailed map when coverage overlaps.
            double dx = x - mapData.centerX;
            double dz = z - mapData.centerZ;
            double centerDistanceSq = dx * dx + dz * dz;
            int scaleFactor = 1 << mapData.scale;

            if (scaleFactor < bestScaleFactor
                    || (scaleFactor == bestScaleFactor && centerDistanceSq < bestCenterDistanceSq)) {
                bestMapId = rawId;
                bestScaleFactor = scaleFactor;
                bestCenterDistanceSq = centerDistanceSq;
            }
        }

        return bestMapId;
    }

    private static boolean mapCoversPosition(Level level, double x, double z, int rawMapId) {
        return mapContainsPosition(level, x, z, level.getMapData(new MapId(rawMapId)));
    }

    private static boolean mapContainsPosition(Level level, double x, double z, @Nullable MapItemSavedData mapData) {
        if (mapData == null) {
            return false;
        }

        // Never select atlas maps from another dimension.
        if (!mapData.dimension.equals(level.dimension())) {
            return false;
        }

        int scaleFactor = 1 << mapData.scale;
        double mapMinX = mapData.centerX - 64.0 * scaleFactor;
        double mapMinZ = mapData.centerZ - 64.0 * scaleFactor;
        double mapMaxX = mapData.centerX + 64.0 * scaleFactor;
        double mapMaxZ = mapData.centerZ + 64.0 * scaleFactor;
        return x >= mapMinX && x < mapMaxX && z >= mapMinZ && z < mapMaxZ;
    }
}
