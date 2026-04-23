package rubbertoe.simple_atlas.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class SimpleAtlasLangProvider extends FabricLanguageProvider {

    public SimpleAtlasLangProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, "en_us", registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.@NonNull Provider registryLookup, TranslationBuilder translationBuilder) {
        translationBuilder.add(ModItems.ATLAS, "Atlas");
        translationBuilder.add("screen.simple_atlas.atlas.title", "Atlas");
        translationBuilder.add("gui.simple_atlas.waypoint_name", "Waypoint name");
        translationBuilder.add("gui.simple_atlas.cancel", "Cancel");
        translationBuilder.add("gui.simple_atlas.confirm", "Confirm");
        translationBuilder.add("gui.simple_atlas.waypoint.edit_title", "Edit Waypoint");
        translationBuilder.add("gui.simple_atlas.waypoint.new_title", "New Waypoint");
        translationBuilder.add("gui.simple_atlas.waypoint.default_name", "Waypoint");
        translationBuilder.add("menu.simple_atlas.teleport", "Teleport");
        translationBuilder.add("menu.simple_atlas.copy_coordinates", "Copy coordinates");
        translationBuilder.add("menu.simple_atlas.waypoint.locate", "Locate");
        translationBuilder.add("menu.simple_atlas.waypoint.stop_locating", "Stop Locating");
        translationBuilder.add("menu.simple_atlas.waypoint.edit", "Edit waypoint");
        translationBuilder.add("menu.simple_atlas.waypoint.delete", "Delete waypoint");
        translationBuilder.add("menu.simple_atlas.map.new_waypoint", "New waypoint");
        translationBuilder.add("menu.simple_atlas.map.remove", "Remove map from atlas");
        translationBuilder.add("message.simple_atlas.coordinates_copied", "Copied coordinates: %s, %s");
        translationBuilder.add("message.simple_atlas.waypoint_limit_reached", "Atlas waypoint limit reached (%s)");
        translationBuilder.add("message.simple_atlas.no_maps_inserted", "Your atlas has no maps inserted");
        translationBuilder.add("tooltip.simple_atlas.no_maps", "No maps inserted");
        translationBuilder.add("tooltip.simple_atlas.scale", "Scale: (1:%s)");
        translationBuilder.add("advancements.simple-atlas.adventure.craft_atlas.title", "Paper Trail");
        translationBuilder.add("advancements.simple-atlas.adventure.craft_atlas.description", "Obtain an empty Atlas");
        translationBuilder.add("advancements.simple-atlas.adventure.old_fashioned.title", "Old Fashioned");
        translationBuilder.add("advancements.simple-atlas.adventure.old_fashioned.description", "Add a waypoint to an Atlas using a banner");
        translationBuilder.add("advancements.simple-atlas.adventure.backup_copy.title", "Backup Copy");
        translationBuilder.add("advancements.simple-atlas.adventure.backup_copy.description", "Duplicate an Atlas with a book at a cartography table");
        translationBuilder.add("advancements.simple-atlas.adventure.better_together.title", "Better Together");
        translationBuilder.add("advancements.simple-atlas.adventure.better_together.description", "Merge two Atlases at a cartography table");
        translationBuilder.add("advancements.simple-atlas.adventure.bigger_picture.title", "Bigger Picture");
        translationBuilder.add("advancements.simple-atlas.adventure.bigger_picture.description", "Upscale an Atlas with paper at a cartography table");
        translationBuilder.add("advancements.simple-atlas.adventure.marco.title", "Marco!");
        translationBuilder.add("advancements.simple-atlas.adventure.marco.description", "Pin a waypoint to the locator bar");
        translationBuilder.add("key.category.simple-atlas.atlas", "Simple Atlas");
        translationBuilder.add("key.simple_atlas.reset_zoom", "Reset Zoom");
    }
}
