package rubbertoe.simple_atlas.navigation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public final class NavigationCompassUtil {
    public static final String NAV_COMPASS_FLAG_KEY = "simple_atlas_nav_compass";
    public static final String NAV_COMPASS_OWNER_KEY = "simple_atlas_nav_owner";

    private NavigationCompassUtil() {}

    public static boolean isNavigationCompass(ItemStack stack) {
        if (!stack.is(Items.COMPASS)) {
            return false;
        }

        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getBoolean(NAV_COMPASS_FLAG_KEY).orElse(false);
    }

    public static boolean isOwnedBy(ItemStack stack, UUID playerId) {
        if (!isNavigationCompass(stack)) {
            return false;
        }

        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return playerId.toString().equals(data.copyTag().getString(NAV_COMPASS_OWNER_KEY).orElse(""));
    }

    public static int countOwnedNavigationCompasses(Inventory inventory, UUID playerId) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isOwnedBy(inventory.getItem(i), playerId)) {
                count++;
            }
        }
        return count;
    }
}


