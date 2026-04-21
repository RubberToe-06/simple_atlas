package rubbertoe.simple_atlas.advancement;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.advancements.CriterionTrigger;
import rubbertoe.simple_atlas.SimpleAtlas;

public final class ModCriteria {
    private ModCriteria() {}

    public static final AtlasCartographyActionTrigger ATLAS_CARTOGRAPHY_ACTION = register(
            "atlas_cartography_action",
            AtlasCartographyActionTrigger.INSTANCE
    );

    public static final WaypointAddedViaBannerTrigger WAYPOINT_ADDED_VIA_BANNER = register(
            "waypoint_added_via_banner",
            new WaypointAddedViaBannerTrigger()
    );

    public static final WaypointPinnedToLocatorBarTrigger WAYPOINT_PINNED_TO_LOCATOR_BAR = register(
            "waypoint_pinned_to_locator_bar",
            new WaypointPinnedToLocatorBarTrigger()
    );

    private static <T extends CriterionTrigger<?>> T register(String name, T criterion) {
        return Registry.register(BuiltInRegistries.TRIGGER_TYPES, Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, name), criterion);
    }

    public static void initialize() {
        // no-op: class load triggers registration
    }
}


