package rubbertoe.simple_atlas.map;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class AtlasMapSelector {
    private AtlasMapSelector() {}

    public static @Nullable Integer findCurrentMapRawId(Level level, double x, double z, List<Integer> mapIds) {
        Integer bestMapId = null;
        int bestScaleFactor = Integer.MAX_VALUE;
        double bestCenterDistanceSq = Double.MAX_VALUE;

        for (int rawId : mapIds) {
            MapItemSavedData mapData = level.getMapData(new MapId(rawId));
            if (mapData == null) {
                continue;
            }

            // Never select atlas maps from another dimension.
            if (!mapData.dimension.equals(level.dimension())) {
                continue;
            }

            int scaleFactor = 1 << mapData.scale;
            double mapMinX = mapData.centerX - 64.0 * scaleFactor;
            double mapMinZ = mapData.centerZ - 64.0 * scaleFactor;
            double mapMaxX = mapData.centerX + 64.0 * scaleFactor;
            double mapMaxZ = mapData.centerZ + 64.0 * scaleFactor;

            if (x >= mapMinX && x < mapMaxX && z >= mapMinZ && z < mapMaxZ) {
                // Prefer the most detailed map when coverage overlaps.
                double dx = x - mapData.centerX;
                double dz = z - mapData.centerZ;
                double centerDistanceSq = dx * dx + dz * dz;

                if (scaleFactor < bestScaleFactor
                        || (scaleFactor == bestScaleFactor && centerDistanceSq < bestCenterDistanceSq)) {
                    bestMapId = rawId;
                    bestScaleFactor = scaleFactor;
                    bestCenterDistanceSq = centerDistanceSq;
                }
            }
        }

        return bestMapId;
    }
}
