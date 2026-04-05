package rubbertoe.simple_atlas.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AtlasViewTicker {
    private static final int SYNC_INTERVAL_TICKS = 10;

    private AtlasViewTicker() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(AtlasViewTicker::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) ->
                AtlasViewManager.stopViewing(handler.player)
        );
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        for (Map.Entry<UUID, List<Integer>> entry : AtlasViewManager.entries()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }

            Integer currentMapRawId = findCurrentMapIdForPlayer(player, entry.getValue());
            if (currentMapRawId == null) {
                continue;
            }

            MapId mapId = new MapId(currentMapRawId);
            MapItemSavedData mapData = player.level().getMapData(mapId);
            if (mapData == null) {
                continue;
            }

            // Mirror vanilla map update behavior while atlas is open, but only for the map under the player.
            if (!mapData.locked) {
                ((MapItem) Items.FILLED_MAP).update(player.level(), player, mapData);
            }

            mapData.getHoldingPlayer(player);
            Packet<?> packet = mapData.getUpdatePacket(mapId, player);
            if (packet != null) {
                player.connection.send(packet);
            }
        }
    }

    private static Integer findCurrentMapIdForPlayer(ServerPlayer player, List<Integer> mapIds) {
        double x = player.getX();
        double z = player.getZ();

        for (int rawId : mapIds) {
            MapItemSavedData mapData = player.level().getMapData(new MapId(rawId));
            if (mapData == null) {
                continue;
            }

            int scaleFactor = 1 << mapData.scale;
            double mapMinX = mapData.centerX - 64.0 * scaleFactor;
            double mapMinZ = mapData.centerZ - 64.0 * scaleFactor;
            double mapMaxX = mapData.centerX + 64.0 * scaleFactor;
            double mapMaxZ = mapData.centerZ + 64.0 * scaleFactor;

            if (x >= mapMinX && x < mapMaxX && z >= mapMinZ && z < mapMaxZ) {
                return rawId;
            }
        }

        return null;
    }
}