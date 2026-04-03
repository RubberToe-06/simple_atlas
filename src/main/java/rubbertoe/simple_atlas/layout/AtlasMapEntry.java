package rubbertoe.simple_atlas.layout;

public record AtlasMapEntry(
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

