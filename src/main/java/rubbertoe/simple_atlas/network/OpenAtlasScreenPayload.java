package rubbertoe.simple_atlas.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.SimpleAtlas;

import java.util.ArrayList;
import java.util.List;

public record OpenAtlasScreenPayload(
        List<AtlasTilePayload> tiles
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "open_atlas_screen");
    public static final Type<OpenAtlasScreenPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, AtlasTilePayload> TILE_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, AtlasTilePayload::mapId,
                    ByteBufCodecs.INT, AtlasTilePayload::centerX,
                    ByteBufCodecs.INT, AtlasTilePayload::centerZ,
                    ByteBufCodecs.INT, AtlasTilePayload::tileX,
                    ByteBufCodecs.INT, AtlasTilePayload::tileY,
                    AtlasTilePayload::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAtlasScreenPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, TILE_CODEC),
                    p -> new ArrayList<>(p.tiles()),
                    OpenAtlasScreenPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

