package rubbertoe.simple_atlas.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import rubbertoe.simple_atlas.navigation.NavigationCompassUtil;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;
import rubbertoe.simple_atlas.item.ModItems;
import rubbertoe.simple_atlas.server.AtlasViewManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        PayloadTypeRegistry.serverboundPlay().register(
                NavigateToWaypointPayload.TYPE,
                NavigateToWaypointPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                StopNavigatingPayload.TYPE,
                StopNavigatingPayload.CODEC
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
        ServerPlayNetworking.registerGlobalReceiver(
                NavigateToWaypointPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();

                    // Keep only one special navigation compass at a time.
                    ItemStack offhand = player.getOffhandItem();
                    if (!offhand.isEmpty() && !isNavigationCompass(offhand)) {
                        ItemStack displaced = offhand.copy();
                        player.getInventory().setItem(40, ItemStack.EMPTY);
                        if (!player.getInventory().add(displaced)) {
                            player.drop(displaced, false);
                        }
                    }

                    removeNavigationCompasses(player);

                    ItemStack compass = new ItemStack(Items.COMPASS);

                    // Set custom name via component
                    compass.set(DataComponents.CUSTOM_NAME,
                            Component.literal("Navigate to: " + payload.waypointName()));
                    CustomData.update(DataComponents.CUSTOM_DATA, compass, tag -> {
                        tag.putBoolean(NavigationCompassUtil.NAV_COMPASS_FLAG_KEY, true);
                        tag.putString(NavigationCompassUtil.NAV_COMPASS_OWNER_KEY, player.getUUID().toString());
                    });

                    // Set lodestone tracker to point to waypoint coordinates
                    BlockPos pos = BlockPos.containing(
                            payload.worldX(), 64.0, payload.worldZ()
                    );

                    var level = context.server().getLevel(Level.OVERWORLD);
                    if (level == null) {
                        return;
                    }
                    GlobalPos target = GlobalPos.of(
                            level.dimension(),
                            pos
                    );

                    var lodestoneTracker = new LodestoneTracker(
                            Optional.of(target),
                            false
                    );
                    compass.set(DataComponents.LODESTONE_TRACKER, lodestoneTracker);

                    // Place compass in offhand (Inventory.SLOT_OFFHAND = 40)
                    player.getInventory().setItem(40, compass);
                    player.getInventory().setChanged();

                    // Force inventory sync to client
                    player.containerMenu.broadcastChanges();
                    player.connection.send(player.getInventory().createInventoryUpdatePacket(40));
                })
        );
        ServerPlayNetworking.registerGlobalReceiver(
                StopNavigatingPayload.TYPE,
                (_, context) -> context.server().execute(() -> {
                    var player = context.player();
                    removeNavigationCompasses(player);
                    player.getInventory().setChanged();
                    player.containerMenu.broadcastChanges();
                    player.connection.send(player.getInventory().createInventoryUpdatePacket(40));
                })
        );
    }

    private static boolean isNavigationCompass(ItemStack stack) {
        return NavigationCompassUtil.isNavigationCompass(stack);
    }

    private static void removeNavigationCompasses(net.minecraft.server.level.ServerPlayer player) {
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isNavigationCompass(stack)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
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