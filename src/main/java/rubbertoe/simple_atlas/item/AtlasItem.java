package rubbertoe.simple_atlas.item;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;
import rubbertoe.simple_atlas.layout.AtlasLayout;
import rubbertoe.simple_atlas.layout.AtlasLayoutBuilder;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;
import rubbertoe.simple_atlas.navigation.WaypointIconCatalog;
import rubbertoe.simple_atlas.network.AtlasTilePayload;
import rubbertoe.simple_atlas.network.ModNetworking;
import rubbertoe.simple_atlas.network.OpenAtlasScreenPayload;
import rubbertoe.simple_atlas.map.AtlasMapSelector;
import rubbertoe.simple_atlas.server.AtlasViewManager;

public class AtlasItem extends Item {
    public AtlasItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull InteractionResult useOn(@NonNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof BannerBlockEntity banner)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack atlasStack = context.getItemInHand();
        AtlasContents contents = atlasStack.getOrDefault(ModComponents.ATLAS_CONTENTS, AtlasContents.EMPTY);

        if (contents.waypoints().size() >= ModNetworking.MAX_WAYPOINT_COUNT) {
            player.sendSystemMessage(
                    Component.literal("Atlas waypoint limit reached (" + ModNetworking.MAX_WAYPOINT_COUNT + ")")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResult.SUCCESS_SERVER;
        }

        BlockPos bannerPos = context.getClickedPos();
        if (hasWaypointAtBlock(contents, bannerPos)) {
            player.sendSystemMessage(
                    Component.literal("This banner is already saved as a waypoint")
                            .withStyle(ChatFormatting.GRAY)
            );
            return InteractionResult.SUCCESS_SERVER;
        }

        int bannerIconIndex = WaypointIconCatalog.bannerIconIndexForColor(banner.getBaseColor());
        String customName = banner.getCustomName() != null ? banner.getCustomName().getString() : "";
        String waypointName = customName.isBlank() ? "Banner " + contents.nextWaypointNumber() : customName;

        AtlasContents.WaypointData waypoint = new AtlasContents.WaypointData(
                bannerPos.getX() + 0.5,
                bannerPos.getZ() + 0.5,
                waypointName,
                bannerIconIndex
        );

        AtlasContents updated = withAppendedWaypoint(contents, waypoint);
        atlasStack.set(ModComponents.ATLAS_CONTENTS, updated);

        player.sendSystemMessage(
                Component.literal("Added banner waypoint: " + waypoint.name())
                        .withStyle(ChatFormatting.GREEN)
        );
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            @NonNull ItemStack stack,
            Item.@NonNull TooltipContext context,
            @NonNull TooltipDisplay tooltipDisplay,
            @NonNull Consumer<Component> tooltipComponents,
            @NonNull TooltipFlag flag
    ) {
        AtlasContents contents = stack.getOrDefault(ModComponents.ATLAS_CONTENTS, AtlasContents.EMPTY);

        if (contents.mapIds().isEmpty()) {
            tooltipComponents.accept(
                    Component.literal("No maps inserted")
                            .withStyle(ChatFormatting.GRAY)
            );
        } else {

            MapItemSavedData mapData = context.mapData(new MapId(contents.mapIds().getFirst()));
            if (mapData != null) {
                int scaleLevel = mapData.scale;
                int scaleRatio = 1 << scaleLevel;
                tooltipComponents.accept(
                        Component.literal("Scale: "+"(1:" + scaleRatio + ")")
                                .withStyle(ChatFormatting.GRAY)
                );
            }
        }
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        ItemStack atlasStack = player.getItemInHand(hand);
        AtlasContents contents = atlasStack.getOrDefault(
                ModComponents.ATLAS_CONTENTS,
                AtlasContents.EMPTY
        );

        if (contents.mapIds().isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("Your atlas has no maps inserted")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return InteractionResult.SUCCESS;
        }

        syncAtlasMapsToPlayer((ServerPlayer) player, serverLevel, contents);
        AtlasViewManager.startViewing((ServerPlayer) player, contents.mapIds());
        ServerPlayNetworking.send((ServerPlayer) player, createOpenPayload(serverLevel, contents));


        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(@NonNull ItemStack stack, @NonNull ServerLevel level, @NonNull Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof Player player)) {
            return;
        }

        boolean heldInHand = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!heldInHand) {
            removeMapIdIfPresent(stack);
            return;
        }

        AtlasContents contents = stack.getOrDefault(ModComponents.ATLAS_CONTENTS, AtlasContents.EMPTY);
        if (contents.mapIds().isEmpty()) {
            removeMapIdIfPresent(stack);
            return;
        }

        MapId existingId = stack.get(DataComponents.MAP_ID);
        Integer preferredRawId = existingId != null ? existingId.id() : null;
        Integer currentMapRawId = AtlasMapSelector.findCurrentMapRawId(
                level,
                player.getX(),
                player.getZ(),
                contents.mapIds(),
                preferredRawId
        );
        if (currentMapRawId == null) {
            removeMapIdIfPresent(stack);
            return;
        }

        MapId targetId = new MapId(currentMapRawId);
        if (!targetId.equals(existingId)) {
            stack.set(DataComponents.MAP_ID, targetId);
        }

        // Mirror vanilla carried-map behavior so the player marker is present on the held atlas map.
        Items.FILLED_MAP.inventoryTick(stack, level, entity, slot);
    }


    public static OpenAtlasScreenPayload createOpenPayload(ServerLevel level, AtlasContents contents) {
        AtlasLayout layout = AtlasLayoutBuilder.build(level, contents);
        return new OpenAtlasScreenPayload(
                layout.entries().stream()
                        .map(entry -> new AtlasTilePayload(
                                entry.mapId(),
                                entry.centerX(),
                                entry.centerZ(),
                                entry.tileX(),
                                entry.tileY()
                        ))
                        .toList(),
                contents.mapIds(),
                contents.waypoints(),
                contents.selectedWaypointIconIndex(),
                contents.nextWaypointNumber()
        );
    }

    public static void syncAtlasMapsToPlayer(ServerPlayer player, ServerLevel level, AtlasContents contents) {
        for (int rawId : contents.mapIds()) {
            MapId mapId = new MapId(rawId);
            MapItemSavedData mapData = level.getMapData(mapId);

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

    private static boolean hasWaypointAtBlock(AtlasContents contents, BlockPos pos) {
        for (AtlasContents.WaypointData waypoint : contents.waypoints()) {
            if (Mth.floor(waypoint.worldX()) == pos.getX() && Mth.floor(waypoint.worldZ()) == pos.getZ()) {
                return true;
            }
        }
        return false;
    }

    private static AtlasContents withAppendedWaypoint(AtlasContents contents, AtlasContents.WaypointData waypoint) {
        var updatedWaypoints = new java.util.ArrayList<>(contents.waypoints());
        updatedWaypoints.add(waypoint);
        return contents.withWaypointState(
                updatedWaypoints,
                contents.selectedWaypointIconIndex(),
                contents.nextWaypointNumber() + 1
        );
    }

    private static void removeMapIdIfPresent(ItemStack stack) {
        if (stack.has(DataComponents.MAP_ID)) {
            stack.remove(DataComponents.MAP_ID);
        }
    }
}
