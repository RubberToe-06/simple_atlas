package rubbertoe.simple_atlas.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {
    @Invoker("moveItemStackTo")
    boolean simple_atlas$invokeMoveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse);
    @Invoker("broadcastChanges")
    void simple_atlas$invokeBroadcastChanges();
}