package rubbertoe.simple_atlas.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends the cartography table's top (map) slot to also accept blank maps
 * ({@link Items#MAP}), enabling the blank-map-to-atlas storage recipe.
 */
@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$3")
public abstract class CartographyTableMapSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void simple_atlas$allowBlankMap(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.is(Items.MAP)) {
            cir.setReturnValue(true);
        }
    }
}

