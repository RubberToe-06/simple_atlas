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
        translationBuilder.add("advancements.simple-atlas.adventure.craft_atlas.title", "Paper Trail");
        translationBuilder.add("advancements.simple-atlas.adventure.craft_atlas.description", "Obtain an empty Atlas");
        translationBuilder.add("advancements.simple-atlas.adventure.old_fashioned.title", "Old Fashioned");
        translationBuilder.add("advancements.simple-atlas.adventure.old_fashioned.description", "Add a waypoint to an Atlas using a banner");
        translationBuilder.add("advancements.simple-atlas.adventure.backup_copy.title", "Backup Copy");
        translationBuilder.add("advancements.simple-atlas.adventure.backup_copy.description", "Duplicate an Atlas with a book at a cartography table");
        translationBuilder.add("advancements.simple-atlas.adventure.better_together.title", "Better Together");
        translationBuilder.add("advancements.simple-atlas.adventure.better_together.description", "Merge two Atlases at a cartography table");
        translationBuilder.add("advancements.simple-atlas.adventure.marco.title", "Marco!");
        translationBuilder.add("advancements.simple-atlas.adventure.marco.description", "Pin a waypoint to the locator bar");
        translationBuilder.add("key.category.simple-atlas.atlas", "Simple Atlas");
        translationBuilder.add("key.simple_atlas.reset_zoom", "Reset Zoom");
    }
}
