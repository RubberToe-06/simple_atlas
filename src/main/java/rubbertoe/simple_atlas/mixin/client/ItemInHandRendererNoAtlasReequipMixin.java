package rubbertoe.simple_atlas.mixin.client;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rubbertoe.simple_atlas.item.ModItems;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererNoAtlasReequipMixin {
    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void simple_atlas$skipReequipForAtlasComponentChanges(
            ItemStack currentlyVisibleItem,
            ItemStack expectedItem,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (currentlyVisibleItem.is(ModItems.ATLAS) && expectedItem.is(ModItems.ATLAS)) {
            cir.setReturnValue(true);
        }
    }
}
