package rubbertoe.simple_atlas.debug;

public record AtlasMapDebugEntry(
        int mapId,
        int centerX,
        int centerZ,
        int scale,
        int mapSpan,
        int gridX,
        int gridZ,
        int tileX,
        int tileY
) {}