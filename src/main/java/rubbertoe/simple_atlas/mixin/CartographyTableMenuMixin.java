package rubbertoe.simple_atlas.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;
import rubbertoe.simple_atlas.item.ModItems;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin {

    @Shadow @Final private ResultContainer resultContainer;
    @Shadow @Final private ContainerLevelAccess access;

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    private void simple_atlas$setupAtlasResult(
            ItemStack mapStack,
            ItemStack additionalStack,
            ItemStack resultStack,
            CallbackInfo ci
    ) {
        // ── Book + atlas → duplicate the atlas (costs 1 book) ────────────────
        if (mapStack.is(Items.BOOK) && additionalStack.is(ModItems.ATLAS)) {
            ItemStack result = additionalStack.copyWithCount(1);

            if (!ItemStack.matches(result, resultStack)) {
                this.resultContainer.setItem(2, result);
                ((CartographyTableMenu) (Object) this).broadcastChanges();
            }
            ci.cancel();
            return;
        }

        // ── Filled map + atlas → add the map to the atlas ────────────────────
        if (mapStack.is(Items.FILLED_MAP) && additionalStack.is(ModItems.ATLAS)) {
            MapId mapId = mapStack.get(DataComponents.MAP_ID);
            if (mapId == null) {
                simple_atlas$rejectAtlasResult();
                ci.cancel();
                return;
            }

            AtlasContents contents = additionalStack.getOrDefault(ModComponents.ATLAS_CONTENTS, AtlasContents.EMPTY);

            if (contents.contains(mapId.id())) {
                simple_atlas$rejectAtlasResult();
                ci.cancel();
                return;
            }

            this.access.execute((level, _) -> {
                MapItemSavedData newMapData = level.getMapData(mapId);
                if (newMapData == null) {
                    simple_atlas$rejectAtlasResult();
                    return;
                }

                if (!contents.mapIds().isEmpty()) {
                    int originRawId = contents.mapIds().getFirst();
                    MapItemSavedData originMapData = level.getMapData(new MapId(originRawId));
                    if (originMapData == null || originMapData.scale != newMapData.scale) {
                        simple_atlas$rejectAtlasResult();
                        return;
                    }
                }

                ItemStack result = additionalStack.copyWithCount(1);
                result.set(ModComponents.ATLAS_CONTENTS, contents.withAdded(mapId.id()));

                if (!ItemStack.matches(result, resultStack)) {
                    this.resultContainer.setItem(2, result);
                    ((CartographyTableMenu) (Object) this).broadcastChanges();
                }
            });

            ci.cancel();
        }

        // ── Atlas + atlas → merge contents (requires same size) ──────────────
        if (mapStack.is(ModItems.ATLAS) && additionalStack.is(ModItems.ATLAS)) {
            AtlasContents topContents = mapStack.getOrDefault(ModComponents.ATLAS_CONTENTS, AtlasContents.EMPTY);
            AtlasContents bottomContents = additionalStack.getOrDefault(ModComponents.ATLAS_CONTENTS, AtlasContents.EMPTY);

            if (topContents.size() != bottomContents.size()) {
                simple_atlas$rejectAtlasResult();
                ci.cancel();
                return;
            }

            AtlasContents merged = simple_atlas$mergeAtlasContents(bottomContents, topContents);
            ItemStack result = additionalStack.copyWithCount(1);
            result.set(ModComponents.ATLAS_CONTENTS, merged);

            if (!ItemStack.matches(result, resultStack)) {
                this.resultContainer.setItem(2, result);
                ((CartographyTableMenu) (Object) this).broadcastChanges();
            }

            ci.cancel();
        }
    }

    @Unique
    private static AtlasContents simple_atlas$mergeAtlasContents(AtlasContents base, AtlasContents incoming) {
        LinkedHashSet<Integer> mergedMapIds = new LinkedHashSet<>(base.mapIds());
        mergedMapIds.addAll(incoming.mapIds());

        LinkedHashSet<AtlasContents.WaypointData> mergedWaypoints = new LinkedHashSet<>(base.waypoints());
        mergedWaypoints.addAll(incoming.waypoints());

        int selectedIcon = base.selectedWaypointIconIndex();
        int nextWaypointNumber = Math.max(base.nextWaypointNumber(), incoming.nextWaypointNumber());

        return new AtlasContents(
                List.copyOf(mergedMapIds),
                new ArrayList<>(mergedWaypoints),
                selectedIcon,
                nextWaypointNumber,
                0
        );
    }

    @Unique
    private void simple_atlas$rejectAtlasResult() {
        this.resultContainer.removeItemNoUpdate(2);
        ((CartographyTableMenu) (Object) this).broadcastChanges();
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void simple_atlas$quickMoveAtlas(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        Slot slot = ((CartographyTableMenu) (Object) this).slots.get(slotIndex);

        if (slot == null || !slot.hasItem()) {
            return;
        }

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();

        // ── Book from inventory → slot 0 ──────────────────────────────────────
        if (stack.is(Items.BOOK) && slotIndex >= 3 && slotIndex < 39) {
            if (((AbstractContainerMenuInvoker) this).simple_atlas$invokeMoveItemStackTo(stack, 0, 1, false)) {
                if (stack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                }

                slot.setChanged();

                if (stack.getCount() == clicked.getCount()) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }

                slot.onTake(player, stack);
                ((AbstractContainerMenuInvoker) this).simple_atlas$invokeBroadcastChanges();
                cir.setReturnValue(clicked);
                return;
            }
        }

        // ── Atlas from inventory → slot 1, otherwise slot 0 (for atlas merge) ─
        if (stack.is(ModItems.ATLAS) && slotIndex >= 3 && slotIndex < 39) {
            boolean movedToAdditional = ((AbstractContainerMenuInvoker) this).simple_atlas$invokeMoveItemStackTo(stack, 1, 2, false);
            boolean movedToTop = movedToAdditional
                    || ((AbstractContainerMenuInvoker) this).simple_atlas$invokeMoveItemStackTo(stack, 0, 1, false);

            if (!movedToTop) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }

            slot.setChanged();

            if (stack.getCount() == clicked.getCount()) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }

            slot.onTake(player, stack);
            ((AbstractContainerMenuInvoker) this).simple_atlas$invokeBroadcastChanges();
            cir.setReturnValue(clicked);
        }
    }
}
