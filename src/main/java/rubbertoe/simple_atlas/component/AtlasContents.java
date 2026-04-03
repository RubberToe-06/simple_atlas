package rubbertoe.simple_atlas.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record AtlasContents(List<Integer> mapIds) {
    public static final AtlasContents EMPTY = new AtlasContents(List.of());

    public static final Codec<AtlasContents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.listOf()
                            .optionalFieldOf("map_ids", List.of())
                            .forGetter(AtlasContents::mapIds)
            ).apply(instance, AtlasContents::new)
    );

    public AtlasContents {
        mapIds = List.copyOf(mapIds);
    }

    public boolean contains(int mapId) {
        return mapIds.contains(mapId);
    }

    public AtlasContents withAdded(int mapId) {
        if (contains(mapId)) {
            return this;
        }

        List<Integer> updated = new ArrayList<>(mapIds);
        updated.add(mapId);

        // safety dedupe while preserving order
        return new AtlasContents(new ArrayList<>(new LinkedHashSet<>(updated)));
    }

    public int size() {
        return mapIds.size();
    }
}