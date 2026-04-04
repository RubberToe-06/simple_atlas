package rubbertoe.simple_atlas.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rubbertoe.simple_atlas.navigation.NavigationCompassUtil;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void simple_atlas$preventDuplicateNavCompassPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (!NavigationCompassUtil.isOwnedBy(stack, player.getUUID())) {
            return;
        }

        // If player already has one owned navigation compass, block picking up additional copies.
        if (NavigationCompassUtil.countOwnedNavigationCompasses(player.getInventory(), player.getUUID()) > 0) {
            ci.cancel();
        }
    }
}

