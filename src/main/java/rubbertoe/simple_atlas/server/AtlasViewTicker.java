package rubbertoe.simple_atlas.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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

            for (int rawId : entry.getValue()) {
                MapId mapId = new MapId(rawId);
                MapItemSavedData mapData = player.level().getMapData(mapId);

                if (mapData == null) {
                    continue;
                }

                mapData.getHoldingPlayer(player);
                Packet<?> packet = mapData.getUpdatePacket(mapId, player);

                if (packet != null) {
                    player.connection.send(packet);
                }
            }
        }
    }
}