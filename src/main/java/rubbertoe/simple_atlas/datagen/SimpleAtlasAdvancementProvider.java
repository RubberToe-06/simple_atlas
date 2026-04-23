package rubbertoe.simple_atlas.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.SimpleAtlas;
import rubbertoe.simple_atlas.advancement.AtlasCartographyActionTrigger;
import rubbertoe.simple_atlas.advancement.WaypointAddedViaBannerTrigger;
import rubbertoe.simple_atlas.advancement.WaypointPinnedToLocatorBarTrigger;
import rubbertoe.simple_atlas.item.ModItems;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SimpleAtlasAdvancementProvider extends FabricAdvancementProvider {
    public SimpleAtlasAdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generateAdvancement(HolderLookup.@NonNull Provider registryLookup, @NonNull Consumer<AdvancementHolder> consumer) {
        AdvancementHolder craftAtlas = Advancement.Builder.advancement()
                .parent(AdvancementSubProvider.createPlaceholder("minecraft:adventure/root"))
                .display(
                        ModItems.ATLAS,
                        Component.translatable("advancements.simple-atlas.adventure.craft_atlas.title"),
                        Component.translatable("advancements.simple-atlas.adventure.craft_atlas.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("has_atlas", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.ATLAS))
                .save(consumer, Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "adventure/craft_atlas").toString());

        Advancement.Builder.advancement()
                .parent(craftAtlas)
                .display(
                        Items.WHITE_BANNER,
                        Component.translatable("advancements.simple-atlas.adventure.old_fashioned.title"),
                        Component.translatable("advancements.simple-atlas.adventure.old_fashioned.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("added_waypoint_via_banner", WaypointAddedViaBannerTrigger.TriggerInstance.waypointAddedViaBanner())
                .save(consumer, Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "adventure/old_fashioned").toString());

        Advancement.Builder.advancement()
                .parent(craftAtlas)
                .display(
                        Items.BOOK,
                        Component.translatable("advancements.simple-atlas.adventure.backup_copy.title"),
                        Component.translatable("advancements.simple-atlas.adventure.backup_copy.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("duplicated_atlas", AtlasCartographyActionTrigger.TriggerInstance.duplicatedAtlas())
                .save(consumer, Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "adventure/backup_copy").toString());

        Advancement.Builder.advancement()
                .parent(craftAtlas)
                .display(
                        Items.CARTOGRAPHY_TABLE,
                        Component.translatable("advancements.simple-atlas.adventure.better_together.title"),
                        Component.translatable("advancements.simple-atlas.adventure.better_together.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("merged_atlases", AtlasCartographyActionTrigger.TriggerInstance.mergedAtlases())
                .save(consumer, Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "adventure/better_together").toString());

        Advancement.Builder.advancement()
                .parent(craftAtlas)
                .display(
                        Items.SPYGLASS,
                        Component.translatable("advancements.simple-atlas.adventure.bigger_picture.title"),
                        Component.translatable("advancements.simple-atlas.adventure.bigger_picture.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("scaled_atlas", AtlasCartographyActionTrigger.TriggerInstance.scaledAtlas())
                .save(consumer, Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "adventure/bigger_picture").toString());

        Advancement.Builder.advancement()
                .parent(craftAtlas)
                .display(
                        Items.COMPASS,
                        Component.translatable("advancements.simple-atlas.adventure.marco.title"),
                        Component.translatable("advancements.simple-atlas.adventure.marco.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("pinned_waypoint", WaypointPinnedToLocatorBarTrigger.TriggerInstance.waypointPinnedToLocatorBar())
                .save(consumer, Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "adventure/marco").toString());
    }
}
