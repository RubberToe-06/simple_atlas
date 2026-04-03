package rubbertoe.simple_atlas.item;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.debug.AtlasDebugLayout;
import rubbertoe.simple_atlas.debug.AtlasLayoutDebugger;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;
import rubbertoe.simple_atlas.network.AtlasDebugTilePayload;
import rubbertoe.simple_atlas.network.OpenAtlasDebugScreenPayload;
import rubbertoe.simple_atlas.server.AtlasViewManager;

public class AtlasItem extends Item {
    public AtlasItem(Properties properties) {
        super(properties);
    }

    private static boolean hasMatchingScale(ServerLevel level, AtlasContents contents, MapId newMapId) {
        if (contents.mapIds().isEmpty()) {
            return true;
        }

        MapItemSavedData newMapData = level.getMapData(newMapId);
        if (newMapData == null) {
            return false;
        }

        Integer originRawId = contents.mapIds().getFirst();
        MapItemSavedData originMapData = level.getMapData(new MapId(originRawId));
        if (originMapData == null) {
            return false;
        }

        return newMapData.scale == originMapData.scale;
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

        ItemStack offhandStack = player.getOffhandItem();
        if (!offhandStack.is(Items.FILLED_MAP)) {
            if (contents.mapIds().isEmpty()) {
                player.sendSystemMessage(
                        Component.literal("Your atlas has no maps, add maps using a cartography table")
                                .withStyle(ChatFormatting.YELLOW)
                );
                return InteractionResult.SUCCESS;
            }
            AtlasDebugLayout layout = AtlasLayoutDebugger.build(serverLevel, contents);

            OpenAtlasDebugScreenPayload payload = new OpenAtlasDebugScreenPayload(
                    layout.width(),
                    layout.height(),
                    layout.entries().stream()
                            .map(entry -> new AtlasDebugTilePayload(
                                    entry.mapId(),
                                    entry.centerX(),
                                    entry.centerZ(),
                                    entry.tileX(),
                                    entry.tileY()
                            ))
                            .toList()
            );
            for (int rawId : contents.mapIds()) {
                MapId mapId = new MapId(rawId);
                MapItemSavedData mapData = serverLevel.getMapData(mapId);

                if (mapData == null) {
                    continue;
                }

                mapData.getHoldingPlayer(player);
                Packet<?> packet = mapData.getUpdatePacket(mapId, player);

                if (packet != null) {
                    ((ServerPlayer) player).connection.send(packet);
                }
            }
            AtlasViewManager.startViewing((ServerPlayer) player, contents.mapIds());
            ServerPlayNetworking.send((ServerPlayer) player, payload);

            return InteractionResult.SUCCESS;
        }

        // Add map from offhand
        MapId mapIdComponent = offhandStack.get(DataComponents.MAP_ID);
        if (mapIdComponent == null) {
            return InteractionResult.PASS;
        }

        int mapId = mapIdComponent.id();

        // Scale validation
        if (!hasMatchingScale(serverLevel, contents, mapIdComponent)) {
            MapItemSavedData newMapData = serverLevel.getMapData(mapIdComponent);
            MapItemSavedData originMapData = serverLevel.getMapData(new MapId(contents.mapIds().getFirst()));

            if (originMapData != null && newMapData != null) {
                player.sendSystemMessage(
                        Component.literal(
                                "Map scale mismatch. Atlas scale: " + originMapData.scale +
                                        ", new map scale: " + newMapData.scale
                        ).withStyle(ChatFormatting.RED)
                );
            }

            return InteractionResult.SUCCESS;
        }

        // Duplicate check
        if (contents.contains(mapId)) {
            player.sendSystemMessage(
                    Component.literal("Atlas already contains map #" + mapId)
                            .withStyle(ChatFormatting.YELLOW)
            );
            return InteractionResult.SUCCESS;
        }

        // Add map
        AtlasContents updated = contents.withAdded(mapId);
        atlasStack.set(ModComponents.ATLAS_CONTENTS, updated);

        player.sendSystemMessage(
                Component.literal("Added map #" + mapId + " to atlas (" + updated.size() + " total)")
                        .withStyle(ChatFormatting.GREEN)
        );

        return InteractionResult.SUCCESS;
    }
}