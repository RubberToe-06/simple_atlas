package rubbertoe.simple_atlas.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;
import rubbertoe.simple_atlas.item.ModItems;

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
        if (!mapStack.is(Items.FILLED_MAP)) {
            return;
        }

        if (!additionalStack.is(ModItems.ATLAS)) {
            return;
        }

        MapId mapId = mapStack.get(DataComponents.MAP_ID);
        if (mapId == null) {
            this.resultContainer.removeItemNoUpdate(2);
            ((CartographyTableMenu) (Object) this).broadcastChanges();
            ci.cancel();
            return;
        }

        AtlasContents contents = additionalStack.getOrDefault(
                ModComponents.ATLAS_CONTENTS,
                AtlasContents.EMPTY
        );

        if (contents.contains(mapId.id())) {
            this.resultContainer.removeItemNoUpdate(2);
            ((CartographyTableMenu) (Object) this).broadcastChanges();
            ci.cancel();
            return;
        }

        this.access.execute((level, _) -> {
            MapItemSavedData newMapData = MapItem.getSavedData(mapStack, level);
            if (newMapData == null) {
                this.resultContainer.removeItemNoUpdate(2);
                ((CartographyTableMenu) (Object) this).broadcastChanges();
                return;
            }

            if (!contents.mapIds().isEmpty()) {
                int originRawId = contents.mapIds().getFirst();

                ItemStack originMapStack = new ItemStack(Items.FILLED_MAP);
                originMapStack.set(DataComponents.MAP_ID, new MapId(originRawId));

                MapItemSavedData originMapData = MapItem.getSavedData(originMapStack, level);
                if (originMapData == null || originMapData.scale != newMapData.scale) {
                    this.resultContainer.removeItemNoUpdate(2);
                    ((CartographyTableMenu) (Object) this).broadcastChanges();
                    return;
                }
            }

            AtlasContents updated = contents.withAdded(mapId.id());

            ItemStack result = additionalStack.copyWithCount(1);
            result.set(ModComponents.ATLAS_CONTENTS, updated);

            if (!ItemStack.matches(result, resultStack)) {
                this.resultContainer.setItem(2, result);
                ((CartographyTableMenu) (Object) this).broadcastChanges();
            }
        });

        ci.cancel();
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

        if (stack.is(ModItems.ATLAS) && slotIndex >= 3 && slotIndex < 39) {
            if (!((AbstractContainerMenuInvoker) this).simple_atlas$invokeMoveItemStackTo(stack, 1, 2, false)) {
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