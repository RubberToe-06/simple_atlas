package rubbertoe.simple_atlas.server;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AtlasViewManager {
    private AtlasViewManager() {}

    private static final Map<UUID, List<Integer>> ACTIVE_VIEWS = new ConcurrentHashMap<>();

    public static void startViewing(ServerPlayer player, List<Integer> mapIds) {
        ACTIVE_VIEWS.put(player.getUUID(), List.copyOf(mapIds));
    }

    public static void updateViewedMaps(ServerPlayer player, List<Integer> mapIds) {
        ACTIVE_VIEWS.put(player.getUUID(), List.copyOf(mapIds));
    }

    public static void stopViewing(ServerPlayer player) {
        ACTIVE_VIEWS.remove(player.getUUID());
    }

    public static boolean isViewing(ServerPlayer player) {
        return ACTIVE_VIEWS.containsKey(player.getUUID());
    }

}
