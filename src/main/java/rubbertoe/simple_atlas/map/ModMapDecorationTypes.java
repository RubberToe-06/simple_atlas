package rubbertoe.simple_atlas.map;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import rubbertoe.simple_atlas.SimpleAtlas;

public final class ModMapDecorationTypes {
    private ModMapDecorationTypes() {}

    public static final Holder<MapDecorationType> HOME = register("home");
    public static final Holder<MapDecorationType> NETHER_PORTAL = register("nether_portal");
    public static final Holder<MapDecorationType> SKULL = register("skull");
    public static final Holder<MapDecorationType> AXE = register("axe");
    public static final Holder<MapDecorationType> PICKAXE = register("pickaxe");
    public static final Holder<MapDecorationType> SHOVEL = register("shovel");
    public static final Holder<MapDecorationType> SWORD = register("sword");
    public static final Holder<MapDecorationType> COAL = register("coal");
    public static final Holder<MapDecorationType> COPPER_INGOT = register("copper_ingot");
    public static final Holder<MapDecorationType> DIAMOND = register("diamond");
    public static final Holder<MapDecorationType> EMERALD = register("emerald");
    public static final Holder<MapDecorationType> GOLD_INGOT = register("gold_ingot");
    public static final Holder<MapDecorationType> IRON_INGOT = register("iron_ingot");
    public static final Holder<MapDecorationType> LAPIS_LAZULI = register("lapis_lazuli");
    public static final Holder<MapDecorationType> REDSTONE_DUST = register("redstone_dust");

    private static Holder<MapDecorationType> register(String key) {
        Identifier id = Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, key);
        ResourceKey<MapDecorationType> registryKey = ResourceKey.create(Registries.MAP_DECORATION_TYPE, id);
        MapDecorationType type = new MapDecorationType(id, true, -1, false, false);
        return Registry.registerForHolder(BuiltInRegistries.MAP_DECORATION_TYPE, registryKey, type);
    }

    public static void initialize() {
        // no-op; class loading performs registrations
    }
}
