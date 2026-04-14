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
        translationBuilder.add("key.category.simple-atlas.atlas", "Simple Atlas");
        translationBuilder.add("key.simple_atlas.reset_zoom", "Reset Zoom");
    }
}
