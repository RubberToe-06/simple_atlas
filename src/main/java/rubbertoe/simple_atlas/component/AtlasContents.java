package rubbertoe.simple_atlas.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;

public final class AtlasContents {
    public static final AtlasContents EMPTY = new AtlasContents(List.of());

    public static final Codec<AtlasContents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.listOf()
                            .optionalFieldOf("map_ids", List.of())
                            .forGetter(AtlasContents::mapIds)
            ).apply(instance, AtlasContents::new)
    );

    // LinkedHashSet: O(1) contains() + insertion-order iteration
    private final SequencedSet<Integer> mapIdSet;

    public AtlasContents(List<Integer> mapIds) {
        this.mapIdSet = new LinkedHashSet<>(mapIds);
    }

    /** Returns map IDs in insertion order. */
    public List<Integer> mapIds() {
        return List.copyOf(mapIdSet);
    }

    /** O(1) membership check. */
    public boolean contains(int mapId) {
        return mapIdSet.contains(mapId);
    }

    /** Returns a new {@code AtlasContents} with {@code mapId} appended, or {@code this} if already present. */
    public AtlasContents withAdded(int mapId) {
        if (contains(mapId)) {
            return this;
        }

        LinkedHashSet<Integer> updated = new LinkedHashSet<>(mapIdSet);
        updated.add(mapId);
        return new AtlasContents(List.copyOf(updated));
    }

    public int size() {
        return mapIdSet.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasContents other)) return false;
        return mapIdSet.equals(other.mapIdSet);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mapIdSet);
    }

    @Override
    public String toString() {
        return "AtlasContents{mapIds=" + mapIdSet + "}";
    }
}