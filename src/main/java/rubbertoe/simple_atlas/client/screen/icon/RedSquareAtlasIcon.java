package rubbertoe.simple_atlas.client.screen.icon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class RedSquareAtlasIcon extends StaticAtlasIcon {
    private final int color;

    public RedSquareAtlasIcon(
            Identifier texture,
            int renderWidth,
            int renderHeight,
            double worldX,
            double worldZ,
            @Nullable Component hoverTitle,
            int color
    ) {
        super(texture, 1, 1, renderWidth, renderHeight, worldX, worldZ, hoverTitle, 0.0f);
        this.color = color;
    }

    @Override
    protected void renderAtAnchor(GuiGraphicsExtractor graphics, Minecraft minecraft, Anchor anchor) {
        int halfWidth = renderWidth() / 2;
        int halfHeight = renderHeight() / 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate(anchor.screenX(), anchor.screenY());
        graphics.fill(-halfWidth, -halfHeight, -halfWidth + renderWidth(), -halfHeight + renderHeight(), color);
        graphics.pose().popMatrix();
    }
}

