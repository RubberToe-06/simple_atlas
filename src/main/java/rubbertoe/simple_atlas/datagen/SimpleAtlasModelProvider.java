package rubbertoe.simple_atlas.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.BlockModelGenerators;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.item.ModItems;

public class SimpleAtlasModelProvider extends FabricModelProvider {
    public SimpleAtlasModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(@NonNull BlockModelGenerators blockModelGenerator) {
        // no blocks yet
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(ModItems.ATLAS, ModelTemplates.FLAT_ITEM);
    }
}