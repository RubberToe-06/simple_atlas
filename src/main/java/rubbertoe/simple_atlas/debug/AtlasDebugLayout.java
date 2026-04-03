package rubbertoe.simple_atlas.debug;

import java.util.List;

public record AtlasDebugLayout(
        List<AtlasMapDebugEntry> entries,
        int originMapId,
        int originCenterX,
        int originCenterZ,
        int originScale,
        int mapSpan,
        int minGridX,
        int maxGridX,
        int minGridZ,
        int maxGridZ,
        int width,
        int height
) {}