package rubbertoe.simple_atlas.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.waypoints.Waypoint;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;
import rubbertoe.simple_atlas.item.ModItems;
import rubbertoe.simple_atlas.navigation.WaypointIconCatalog;
import rubbertoe.simple_atlas.server.AtlasViewManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModNetworking {
    private static final int MAX_WAYPOINT_COUNT = 256;
    private static final Map<UUID, Set<UUID>> PINNED_NAVIGATION_IDS = new ConcurrentHashMap<>();

    private ModNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(
                OpenAtlasScreenPayload.TYPE,
                OpenAtlasScreenPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                CloseAtlasViewPayload.TYPE,
                CloseAtlasViewPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                SaveAtlasWaypointsPayload.TYPE,
                SaveAtlasWaypointsPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                NavigateToWaypointPayload.TYPE,
                NavigateToWaypointPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                StopNavigatingPayload.TYPE,
                StopNavigatingPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                UnpinWaypointPayload.TYPE,
                UnpinWaypointPayload.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                CloseAtlasViewPayload.TYPE,
                (_, context) -> context.server().execute(() -> AtlasViewManager.stopViewing(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(
                SaveAtlasWaypointsPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();
                    ItemStack atlasStack = player.getMainHandItem();
                    if (!atlasStack.is(ModItems.ATLAS)) {
                        return;
                    }

                    AtlasContents contents = atlasStack.getOrDefault(ModComponents.ATLAS_CONTENTS, AtlasContents.EMPTY);
                    if (!contents.mapIds().equals(payload.atlasMapIds())) {
                        return;
                    }

                    List<AtlasContents.WaypointData> sanitizedWaypoints = sanitizeWaypoints(payload.waypoints());
                    AtlasContents updated = contents.withWaypointState(
                            sanitizedWaypoints,
                            payload.selectedWaypointIconIndex(),
                            payload.nextWaypointNumber()
                    );
                    atlasStack.set(ModComponents.ATLAS_CONTENTS, updated);

                    // Keep locator-bar pins in sync with saved waypoint data.
                    Set<UUID> pinnedIds = PINNED_NAVIGATION_IDS.get(player.getUUID());
                    if (pinnedIds == null || pinnedIds.isEmpty()) {
                        return;
                    }

                    Set<UUID> validWaypointIds = new HashSet<>();
                    for (AtlasContents.WaypointData waypoint : sanitizedWaypoints) {
                        validWaypointIds.add(WaypointIconCatalog.navigationWaypointId(waypoint.worldX(), waypoint.worldZ()));
                    }

                    List<UUID> stalePinnedIds = pinnedIds.stream()
                            .filter(id -> !validWaypointIds.contains(id))
                            .toList();
                    if (stalePinnedIds.isEmpty()) {
                        return;
                    }

                    stalePinnedIds.forEach(id -> player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(id)));
                    for (UUID staleId : stalePinnedIds) {
                        pinnedIds.remove(staleId);
                    }
                    if (pinnedIds.isEmpty()) {
                        PINNED_NAVIGATION_IDS.remove(player.getUUID());
                    }
                })
        );
        ServerPlayNetworking.registerGlobalReceiver(
                NavigateToWaypointPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();
                    if (!playerHasAtlasInInventory(player)) {
                        clearPinnedWaypoints(player);
                        return;
                    }

                    UUID playerId = player.getUUID();
                    UUID newNavigationId = WaypointIconCatalog.navigationWaypointId(payload.worldX(), payload.worldZ());

                    Set<UUID> pinnedIds = PINNED_NAVIGATION_IDS.computeIfAbsent(playerId, _ -> new HashSet<>());
                    if (!pinnedIds.add(newNavigationId)) {
                        return;
                    }

                    Waypoint.Icon icon = new Waypoint.Icon();
                    icon.style = WaypointIconCatalog.styleKeyForIndex(payload.waypointIconIndex());
                    icon.color = Optional.of(0xFFFFFF);

                    BlockPos pos = BlockPos.containing(payload.worldX(), player.getY(), payload.worldZ());
                    player.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(newNavigationId, icon, pos));
                })
        );
        ServerPlayNetworking.registerGlobalReceiver(
                StopNavigatingPayload.TYPE,
                (_, context) -> context.server().execute(() -> clearPinnedWaypoints(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(
                UnpinWaypointPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();
                    UUID playerId = player.getUUID();
                    UUID unpinNavigationId = WaypointIconCatalog.navigationWaypointId(payload.worldX(), payload.worldZ());

                    Set<UUID> pinnedIds = PINNED_NAVIGATION_IDS.get(playerId);
                    if (pinnedIds == null || !pinnedIds.remove(unpinNavigationId)) {
                        return;
                    }

                    player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(unpinNavigationId));
                    if (pinnedIds.isEmpty()) {
                        PINNED_NAVIGATION_IDS.remove(playerId);
                    }
                })
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) ->
                PINNED_NAVIGATION_IDS.remove(handler.player.getUUID())
        );
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (UUID playerId : new ArrayList<>(PINNED_NAVIGATION_IDS.keySet())) {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    PINNED_NAVIGATION_IDS.remove(playerId);
                    continue;
                }

                if (!playerHasAtlasInInventory(player)) {
                    clearPinnedWaypoints(player);
                }
            }
        });
    }

    private static boolean playerHasAtlasInInventory(net.minecraft.server.level.ServerPlayer player) {
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (player.getInventory().getItem(i).is(ModItems.ATLAS)) {
                return true;
            }
        }
        return false;
    }

    private static void clearPinnedWaypoints(net.minecraft.server.level.ServerPlayer player) {
        Set<UUID> removedNavigationIds = PINNED_NAVIGATION_IDS.remove(player.getUUID());
        if (removedNavigationIds == null || removedNavigationIds.isEmpty()) {
            return;
        }

        removedNavigationIds.forEach(waypointId ->
                player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(waypointId))
        );
    }

    private static List<AtlasContents.WaypointData> sanitizeWaypoints(List<AtlasContents.WaypointData> waypoints) {
        if (waypoints.isEmpty()) {
            return List.of();
        }

        List<AtlasContents.WaypointData> sanitized = new ArrayList<>(Math.min(waypoints.size(), MAX_WAYPOINT_COUNT));
        for (AtlasContents.WaypointData waypoint : waypoints) {
            if (sanitized.size() >= MAX_WAYPOINT_COUNT) {
                break;
            }
            sanitized.add(new AtlasContents.WaypointData(
                    waypoint.worldX(),
                    waypoint.worldZ(),
                    waypoint.name(),
                    waypoint.iconIndex()
            ));
        }
        return sanitized;
    }
}
