package rubbertoe.simple_atlas.client.screen.icon;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class PlayerAtlasIcon extends AtlasIcon {
    public PlayerAtlasIcon(
            Identifier texture,
            int textureWidth,
            int textureHeight,
            int renderWidth,
            int renderHeight
    ) {
        super(texture, textureWidth, textureHeight, renderWidth, renderHeight);
    }

    @Override
    protected WorldPoint getWorldPoint(Minecraft minecraft) {
        if (minecraft.player == null) {
            return null;
        }

        return new WorldPoint(minecraft.player.getX(), minecraft.player.getZ());
    }

    @Override
    protected boolean isVisible(Minecraft minecraft) {
        return minecraft.player != null;
    }

    @Override
    protected @Nullable Component getHoverTitle(Minecraft minecraft) {
        return minecraft.player != null ? minecraft.player.getDisplayName() : null;
    }

    @Override
    protected float getRotationRadians(Minecraft minecraft) {
        if (minecraft.player == null) {
            return 0.0f;
        }

        float yaw = minecraft.player.getYRot();
        int rot = (int) ((yaw + 180.0f) * 16.0f / 360.0f);
        return (float) Math.toRadians(rot * 360.0f / 16.0f);
    }
}

