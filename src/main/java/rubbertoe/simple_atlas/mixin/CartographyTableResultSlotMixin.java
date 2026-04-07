package rubbertoe.simple_atlas.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rubbertoe.simple_atlas.component.ModComponents;

/**
 * Overrides the result slot's {@code onTake} so that when the blank-map
 * storage recipe completes, the entire blank-map stack in slot 0 is consumed
 * rather than just one map.
 *
 * <p>Vanilla's {@code onTake} removes exactly 1 item from slot 0 and 1 from
 * slot 1.  We inject at TAIL (after vanilla removes its 1) and clear any
 * remaining blank maps, because the result already has all of them encoded.</p>
 */
@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$5")
public abstract class CartographyTableResultSlotMixin {

    /** Outer {@link CartographyTableMenu} instance (synthetic {@code this$0}). */
    @Shadow(aliases = "this$0")
    private CartographyTableMenu simple_atlas$outerMenu;

    @Inject(method = "onTake", at = @At("TAIL"))
    private void simple_atlas$clearRemainingBlankMaps(Player player, ItemStack carried, CallbackInfo ci) {
        // Only act when the result was an atlas produced by the blank-map recipe.
        if (!carried.has(ModComponents.ATLAS_CONTENTS)) {
            return;
        }

        // Slot 0: vanilla already removed 1; clear whatever remains.
        ItemStack slot0 = simple_atlas$outerMenu.container.getItem(0);
        if (!slot0.isEmpty() && slot0.is(Items.MAP)) {
            simple_atlas$outerMenu.container.setItem(0, ItemStack.EMPTY);
        }
    }
}

