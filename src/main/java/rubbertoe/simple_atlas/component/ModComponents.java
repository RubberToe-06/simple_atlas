package rubbertoe.simple_atlas.component;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import rubbertoe.simple_atlas.SimpleAtlas;

public final class ModComponents {
    private ModComponents() {}

    public static final DataComponentType<AtlasContents> ATLAS_CONTENTS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "atlas_contents"),
            DataComponentType.<AtlasContents>builder()
                    .persistent(AtlasContents.CODEC)
                    .build()
    );

    public static void initialize() {
        SimpleAtlas.LOGGER.info("Registering components for {}", SimpleAtlas.MOD_ID);
    }
}