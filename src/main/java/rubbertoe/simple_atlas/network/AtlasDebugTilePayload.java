package rubbertoe.simple_atlas.network;

public record AtlasDebugTilePayload(
        int mapId,
        int centerX,
        int centerZ,
        int tileX,
        int tileY
) {}