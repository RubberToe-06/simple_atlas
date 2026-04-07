package rubbertoe.simple_atlas.item;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import rubbertoe.simple_atlas.layout.AtlasLayout;
import rubbertoe.simple_atlas.layout.AtlasLayoutBuilder;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;
import rubbertoe.simple_atlas.network.AtlasTilePayload;
import rubbertoe.simple_atlas.network.OpenAtlasScreenPayload;
import rubbertoe.simple_atlas.server.AtlasViewManager;

public class AtlasItem extends Item {
    public AtlasItem(Properties properties) {
        super(properties);
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
                    Component.literal(contents.blankMapCount() > 0
                                    ? "No maps · Use atlas to create the first map"
                                    : "No maps · Add filled maps or store blank maps")
                            .withStyle(ChatFormatting.GRAY)
            );
        } else {
            int count = contents.size();
            tooltipComponents.accept(
                    Component.literal("Maps: " + count)
                            .withStyle(ChatFormatting.GRAY)
            );

            // TooltipContext.mapData() gives us map data directly — no level reference needed
            MapItemSavedData mapData = context.mapData(new MapId(contents.mapIds().getFirst()));
            if (mapData != null) {
                tooltipComponents.accept(
                        Component.literal("Scale: " + mapData.scale)
                                .withStyle(ChatFormatting.GRAY)
                );
            }
        }

        if (contents.blankMapCount() > 0) {
            tooltipComponents.accept(
                    Component.literal("Stored blank maps: " + contents.blankMapCount())
                            .withStyle(ChatFormatting.GRAY)
            );
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

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.MAP)) {
            int blankMapsAdded = offhand.getCount();
            contents = contents.withAddedBlankMaps(blankMapsAdded);
            atlasStack.set(ModComponents.ATLAS_CONTENTS, contents);

            if (!player.getAbilities().instabuild) {
                offhand.shrink(blankMapsAdded);
            }

            player.sendSystemMessage(
                    Component.literal("Stored " + blankMapsAdded + " blank map" + (blankMapsAdded == 1 ? "" : "s") + " in the atlas")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return InteractionResult.SUCCESS;
        }

        if (contents.mapIds().isEmpty() && contents.blankMapCount() > 0 && player instanceof ServerPlayer serverPlayer) {
            AtlasContents expanded = tryAppendBlankMapForPlayerPosition(serverLevel, serverPlayer, atlasStack, contents);
            if (expanded != null) {
                contents = expanded;
            }
        }

        if (contents.mapIds().isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("Your atlas has no maps. Add a filled map or store blank maps in it first")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return InteractionResult.SUCCESS;
        }

        syncAtlasMapsToPlayer((ServerPlayer) player, serverLevel, contents);
        AtlasViewManager.startViewing((ServerPlayer) player, contents.mapIds());
        ServerPlayNetworking.send((ServerPlayer) player, createOpenPayload(serverLevel, contents));


        return InteractionResult.SUCCESS;
    }

    public static @Nullable AtlasContents tryAppendBlankMapForPlayerPosition(
            ServerLevel level,
            ServerPlayer player,
            ItemStack atlasStack,
            AtlasContents contents
    ) {
        if (contents.blankMapCount() <= 0) {
            return null;
        }

        int scale = 0;
        if (!contents.mapIds().isEmpty()) {
            MapItemSavedData firstMapData = level.getMapData(new MapId(contents.mapIds().getFirst()));
            if (firstMapData == null) {
                return null;
            }
            scale = firstMapData.scale;
        }

        ItemStack createdMap = MapItem.create(
                level,
                Mth.floor(player.getX()),
                Mth.floor(player.getZ()),
                (byte) scale,
                true,
                false
        );
        MapId createdMapId = createdMap.get(DataComponents.MAP_ID);
        if (createdMapId == null) {
            return null;
        }

        AtlasContents updated = contents.withConsumedBlankMap().withAdded(createdMapId.id());
        atlasStack.set(ModComponents.ATLAS_CONTENTS, updated);
        return updated;
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
}
