package rubbertoe.simple_atlas.client.screen.icon;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class StaticAtlasIcon extends AtlasIcon {
    private final double worldX;
    private final double worldZ;
    private final float rotationRadians;
    private final @Nullable Component hoverTitle;

    public StaticAtlasIcon(
            Identifier texture,
            int textureWidth,
            int textureHeight,
            int renderWidth,
            int renderHeight,
            double worldX,
            double worldZ,
            @Nullable Component hoverTitle
    ) {
        this(texture, textureWidth, textureHeight, renderWidth, renderHeight, worldX, worldZ, hoverTitle, 0.0f);
    }

    public StaticAtlasIcon(
            Identifier texture,
            int textureWidth,
            int textureHeight,
            int renderWidth,
            int renderHeight,
            double worldX,
            double worldZ,
            @Nullable Component hoverTitle,
            float rotationRadians
    ) {
        super(texture, textureWidth, textureHeight, renderWidth, renderHeight);
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.hoverTitle = hoverTitle;
        this.rotationRadians = rotationRadians;
    }

    @Override
    protected WorldPoint getWorldPoint(Minecraft minecraft) {
        return new WorldPoint(worldX, worldZ);
    }

    @Override
    protected float getRotationRadians(Minecraft minecraft) {
        return rotationRadians;
    }

    @Override
    protected @Nullable Component getHoverTitle(Minecraft minecraft) {
        return hoverTitle;
    }
}

