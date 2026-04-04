package rubbertoe.simple_atlas.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.ItemStack;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;
import rubbertoe.simple_atlas.item.ModItems;
import rubbertoe.simple_atlas.server.AtlasViewManager;

import java.util.ArrayList;
import java.util.List;

public final class ModNetworking {
    private static final int MAX_WAYPOINT_COUNT = 256;

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
        ServerPlayNetworking.registerGlobalReceiver(
                CloseAtlasViewPayload.TYPE,
                (_, context) -> context.server().execute(() -> AtlasViewManager.stopViewing(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(
                SaveAtlasWaypointsPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    ItemStack atlasStack = context.player().getMainHandItem();
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
                })
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