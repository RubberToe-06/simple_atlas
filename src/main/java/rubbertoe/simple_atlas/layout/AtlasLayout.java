package rubbertoe.simple_atlas.layout;

import java.util.List;

public record AtlasLayout(
        List<AtlasMapEntry> entries,
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

