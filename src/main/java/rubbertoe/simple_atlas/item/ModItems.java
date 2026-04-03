package rubbertoe.simple_atlas.item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import rubbertoe.simple_atlas.SimpleAtlas;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.component.ModComponents;

import java.util.function.Function;

public final class ModItems {
    private ModItems() {}

    public static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, name)
        );

        T item = factory.apply(properties.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

    public static final Item ATLAS = register(
            "map_atlas",
            AtlasItem::new,
            new Item.Properties()
                    .stacksTo(1)
                    .component(ModComponents.ATLAS_CONTENTS, AtlasContents.EMPTY)
    );

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(ATLAS));
    }
}