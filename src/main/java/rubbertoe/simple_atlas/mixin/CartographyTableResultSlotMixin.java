package rubbertoe.simple_atlas.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rubbertoe.simple_atlas.item.ModItems;

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

    @Unique
    private boolean simple_atlas$bookDuplicationTake;

    @Unique
    private boolean simple_atlas$blankMapStorageTake;

    @Unique
    private ItemStack simple_atlas$duplicationAtlasTemplate = ItemStack.EMPTY;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void simple_atlas$captureRecipeType(Player player, ItemStack carried, CallbackInfo ci) {
        ItemStack slot0 = simple_atlas$outerMenu.container.getItem(0);
        ItemStack slot1 = simple_atlas$outerMenu.container.getItem(1);

        simple_atlas$bookDuplicationTake = slot0.is(Items.BOOK) && slot1.is(ModItems.ATLAS);
        simple_atlas$blankMapStorageTake = slot0.is(Items.MAP) && slot1.is(ModItems.ATLAS);
        simple_atlas$duplicationAtlasTemplate = simple_atlas$bookDuplicationTake
                ? slot1.copyWithCount(1)
                : ItemStack.EMPTY;
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void simple_atlas$handleAtlasRecipes(Player player, ItemStack carried, CallbackInfo ci) {
        if (simple_atlas$bookDuplicationTake && !simple_atlas$duplicationAtlasTemplate.isEmpty()) {
            ItemStack extraAtlas = simple_atlas$duplicationAtlasTemplate.copy();
            if (!player.getInventory().add(extraAtlas)) {
                player.drop(extraAtlas, false);
            }
        }

        // Blank-map storage recipe: vanilla consumed 1 map from slot 0; consume the remainder.
        if (simple_atlas$blankMapStorageTake) {
            ItemStack slot0 = simple_atlas$outerMenu.container.getItem(0);
            if (slot0.is(Items.MAP)) {
                simple_atlas$outerMenu.container.setItem(0, ItemStack.EMPTY);
            }
        }

        simple_atlas$bookDuplicationTake = false;
        simple_atlas$blankMapStorageTake = false;
        simple_atlas$duplicationAtlasTemplate = ItemStack.EMPTY;
    }
}
