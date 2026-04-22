package rubbertoe.simple_atlas.network;

public record AtlasTilePayload(
        int mapId,
        int centerX,
        int centerZ,
        int tileX,
        int tileY,
        String dimension
) {}

