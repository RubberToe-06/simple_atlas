package rubbertoe.simple_atlas.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.NonNull;
import rubbertoe.simple_atlas.SimpleAtlas;
import rubbertoe.simple_atlas.network.AtlasDebugTilePayload;
import rubbertoe.simple_atlas.network.CloseAtlasViewPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasDebugScreen extends Screen {
    private final Map<Integer, MapRenderState> renderStates = new HashMap<>();
    private static final Identifier PLAYER_MARKER_TEXTURE = Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/player_marker.png");
    private static final int TILE_SIZE = 64;
    private final int atlasWidth;
    private final int atlasHeight;
    private final List<AtlasDebugTilePayload> tiles;
    private double panX = 0;
    private double panY = 0;
    private boolean rightDragging = false;
    private float zoom = 1.0f;
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float ZOOM_STEP = 1.1f;

    public AtlasDebugScreen(int atlasWidth, int atlasHeight, List<AtlasDebugTilePayload> tiles) {
        super(Component.literal("Atlas Debug"));
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.tiles = tiles;
    }

    @Override
    protected void init() {
        super.init();
    }

    private record MarkerAnchor(float screenX, float screenY) {}

    private MarkerAnchor findPlayerMarkerAnchor(float originX, float originY, float scaledTileSize) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || tiles.isEmpty()) {
            return null;
        }

        double playerX = minecraft.player.getX();
        double playerZ = minecraft.player.getZ();

        // All atlas maps are same scale; your add logic enforces that.
        MapItemSavedData firstData = minecraft.level.getMapData(new MapId(tiles.getFirst().mapId()));
        if (firstData == null) {
            return null;
        }

        int scaleFactor = 1 << firstData.scale;
        double bestDistSq = Double.MAX_VALUE;
        AtlasDebugTilePayload bestTile = null;
        float bestLocalX = 0.0f;
        float bestLocalY = 0.0f;

        AtlasDebugTilePayload fallbackTile = null;
        double fallbackDistSq = Double.MAX_VALUE;
        float fallbackLocalX = 0.0f;
        float fallbackLocalY = 0.0f;

        for (AtlasDebugTilePayload tile : tiles) {
            double tileWorldMinX = tile.centerX() - 64.0 * scaleFactor;
            double tileWorldMinZ = tile.centerZ() - 64.0 * scaleFactor;

            double localPixelX = (playerX - tileWorldMinX) / scaleFactor;
            double localPixelY = (playerZ - tileWorldMinZ) / scaleFactor;

            boolean inside =
                    localPixelX >= 0.0 && localPixelX < 128.0 &&
                            localPixelY >= 0.0 && localPixelY < 128.0;

            double dx = playerX - tile.centerX();
            double dz = playerZ - tile.centerZ();
            double distSq = dx * dx + dz * dz;

            if (inside) {
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestTile = tile;
                    bestLocalX = (float) localPixelX;
                    bestLocalY = (float) localPixelY;
                }
            }

            if (distSq < fallbackDistSq) {
                fallbackDistSq = distSq;
                fallbackTile = tile;
                fallbackLocalX = (float) localPixelX;
                fallbackLocalY = (float) localPixelY;
            }
        }

        AtlasDebugTilePayload chosen;
        float localX;
        float localY;

        if (bestTile != null) {
            chosen = bestTile;
            localX = bestLocalX;
            localY = bestLocalY;
        } else if (fallbackTile != null) {
            chosen = fallbackTile;
            localX = fallbackLocalX;
            localY = fallbackLocalY;
        } else {
            return null;
        }

        float tileScreenX = (float) (originX + panX + chosen.tileX() * scaledTileSize);
        float tileScreenY = (float) (originY + panY + chosen.tileY() * scaledTileSize);

        float px = tileScreenX + localX * (scaledTileSize / 128.0f);
        float py = tileScreenY + localY * (scaledTileSize / 128.0f);

        return new MarkerAnchor(px, py);
    }
    private void renderGlobalPlayerMarker(
            GuiGraphicsExtractor graphics,
            float originX,
            float originY,
            float scaledTileSize
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        MarkerAnchor anchor = findPlayerMarkerAnchor(originX, originY, scaledTileSize);
        if (anchor == null) {
            return;
        }

        final int textureSize = 16;
        final int size = 14;

        float yaw = minecraft.player.getYRot();
        int rot = (int) ((yaw + 180.0f) * 16.0f / 360.0f);
        float angleRadians = (float) Math.toRadians(rot * 360.0f / 16.0f);

        graphics.pose().pushMatrix();
        graphics.pose().translate(anchor.screenX(), anchor.screenY());
        graphics.pose().rotate(angleRadians);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                PLAYER_MARKER_TEXTURE,
                -size / 2,
                -size / 2,
                0.0f,
                0.0f,
                size,
                size,
                textureSize,
                textureSize,
                textureSize,
                textureSize
        );

        graphics.pose().popMatrix();
    }

    private void renderMapTile(GuiGraphicsExtractor graphics, int mapId, float x, float y, float scale) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        MapId id = new MapId(mapId);
        MapItemSavedData data = minecraft.level.getMapData(id);
        if (data == null) {
            return;
        }

        MapRenderState state = renderStates.computeIfAbsent(mapId, ignored -> new MapRenderState());

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        minecraft.getMapRenderer().extractRenderState(id, data, state);
        graphics.map(state);

        graphics.pose().popMatrix();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        float scaledTileSize = TILE_SIZE * zoom;

        // anchor atlas around screen center at zoom-neutral origin
        float atlasBaseWidth = atlasWidth * TILE_SIZE;
        float atlasBaseHeight = atlasHeight * TILE_SIZE;

        float originX = (this.width - atlasBaseWidth) / 2.0f;
        float originY = (this.height - atlasBaseHeight) / 2.0f;

        for (AtlasDebugTilePayload tile : tiles) {
            float x = (float) (originX + panX + tile.tileX() * scaledTileSize);
            float y = (float) (originY + panY + tile.tileY() * scaledTileSize);

            renderMapTile(graphics, tile.mapId(), x, y, scaledTileSize / 128.0f);

            float tileScreenSize;
            tileScreenSize = scaledTileSize;
            assert minecraft.level != null;

            boolean hovered =
                    mouseX >= x &&
                            mouseX < x + tileScreenSize &&
                            mouseY >= y &&
                            mouseY < y + tileScreenSize;

            if (hovered) {
                int ix = (int) x;
                int iy = (int) y;
                int isize = (int) Math.ceil(tileScreenSize);

                // subtle translucent white wash
                graphics.fill(ix, iy, ix + isize, iy + isize, 0x22FFFFFF);

                // faint outline
                //graphics.outline(ix, iy, isize, isize, 0x66FFFFFF);
            }
        }
        renderGlobalPlayerMarker(graphics, originX, originY, scaledTileSize);
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            rightDragging = true;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (rightDragging && event.button() == 1) {
            panX += dx;
            panY += dy;
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 1) {
            rightDragging = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float oldZoom = this.zoom;
        float newZoom;

        if (scrollY > 0) {
            newZoom = Math.min(MAX_ZOOM, oldZoom * ZOOM_STEP);
        } else if (scrollY < 0) {
            newZoom = Math.max(MIN_ZOOM, oldZoom / ZOOM_STEP);
        } else {
            return false;
        }

        if (newZoom == oldZoom) {
            return true;
        }

        // Must match extractRenderState()
        float atlasBaseWidth = atlasWidth * TILE_SIZE;
        float atlasBaseHeight = atlasHeight * TILE_SIZE;

        float originX = (this.width - atlasBaseWidth) / 2.0f;
        float originY = (this.height - atlasBaseHeight) / 2.0f;

        // atlas-space position under cursor before zoom
        double atlasX = (mouseX - originX - panX) / oldZoom;
        double atlasY = (mouseY - originY - panY) / oldZoom;

        this.zoom = newZoom;

        // keep same atlas-space point under cursor
        this.panX = (float) (mouseX - originX - atlasX * newZoom);
        this.panY = (float) (mouseY - originY - atlasY * newZoom);

        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 82) { // R
            this.zoom = 1.0f;
            this.panX = 0;
            this.panY = 0;
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ClientPlayNetworking.send(new CloseAtlasViewPayload());
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}