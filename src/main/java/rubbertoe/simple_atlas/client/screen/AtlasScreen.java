package rubbertoe.simple_atlas.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import rubbertoe.simple_atlas.SimpleAtlas;
import rubbertoe.simple_atlas.client.screen.icon.AtlasIcon;
import rubbertoe.simple_atlas.client.screen.icon.PlayerAtlasIcon;
import rubbertoe.simple_atlas.client.screen.icon.StaticAtlasIcon;
import rubbertoe.simple_atlas.client.waypoint.AtlasWaypointStore;
import rubbertoe.simple_atlas.network.AtlasTilePayload;
import rubbertoe.simple_atlas.network.CloseAtlasViewPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasScreen extends Screen {
    private final Map<Integer, MapRenderState> renderStates = new HashMap<>();
    private static final Identifier ATLAS_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/atlas_background.png");
    private static final int ATLAS_BACKGROUND_TEXTURE_WIDTH = 256;
    private static final int ATLAS_BACKGROUND_TEXTURE_HEIGHT = 180;
    private static final float BOOK_TARGET_UI_SCALE = 3.0f;
    private static final int BOOK_SCREEN_MARGIN = 16;
    private static final int PAGE_AREA_X = 10;
    private static final int PAGE_AREA_Y = 18;
    private static final int PAGE_AREA_WIDTH = 236;
    private static final int PAGE_AREA_HEIGHT = 143;
    private static final Identifier PLAYER_MARKER_TEXTURE = Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/player_marker.png");
    private static final int PLAYER_MARKER_TEXTURE_SIZE = 14;
    private static final int PLAYER_MARKER_RENDER_SIZE = 12;
    private static final int WAYPOINT_TEXTURE_SIZE = 16;
    private static final int WAYPOINT_RENDER_SIZE = 12;
    private static final int WAYPOINT_NAME_MAX_LENGTH = 32;
    private static final int ICON_HOVER_TITLE_PADDING = 4;
    private static final int TILE_SIZE = 64;
    private final int atlasWidth;
    private final int atlasHeight;
    private final List<AtlasTilePayload> tiles;
    private final PlayerAtlasIcon playerIcon;
    private final List<AtlasIcon> atlasIcons;
    private final List<WaypointIconOption> waypointIconOptions;
    private final AtlasWaypointStore.WaypointState waypointState;
    private int selectedWaypointIconIndex = 0;
    private int nextWaypointNumber = 1;
    private WaypointDraft waypointDraft;
    private double panX = 0;
    private double panY = 0;
    private boolean rightDragging = false;
    private float zoom = 2.0f;
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float ZOOM_STEP = 1.1f;

    private record WaypointIconOption(String name, Identifier texture) {}

    private static List<WaypointIconOption> createWaypointIconOptions() {
        return List.of(
                new WaypointIconOption("Red Banner", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/red_banner.png")),
                new WaypointIconOption("White Banner", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/white_banner.png")),
                new WaypointIconOption("Yellow Banner", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/yellow_banner.png")),
                new WaypointIconOption("Orange Banner", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/orange_banner.png")),
                new WaypointIconOption("Magenta Banner", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/magenta_banner.png")),
                new WaypointIconOption("Lime Banner", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/lime_banner.png")),
                new WaypointIconOption("Nether Portal", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/nether_portal.png")),
                new WaypointIconOption("Ocean Monument", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/ocean_monument.png")),
                new WaypointIconOption("Woodland Mansion", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/woodland_mansion.png")),
                new WaypointIconOption("Trial Chambers", Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/trial_chambers.png"))
        );
    }

    private static class WaypointDraft {
        final double worldX;
        final double worldZ;
        String name;
        int iconIndex;

        WaypointDraft(double worldX, double worldZ, String name, int iconIndex) {
            this.worldX = worldX;
            this.worldZ = worldZ;
            this.name = name;
            this.iconIndex = iconIndex;
        }
    }

    private record WorldPoint(double x, double z) {}

    public AtlasScreen(List<AtlasTilePayload> tiles, AtlasWaypointStore.WaypointState waypointState) {
        super(Component.literal("Atlas"));
        this.tiles = tiles;
        this.waypointState = waypointState;
        this.atlasWidth = tiles.stream().mapToInt(AtlasTilePayload::tileX).max().orElse(0) + 1;
        this.atlasHeight = tiles.stream().mapToInt(AtlasTilePayload::tileY).max().orElse(0) + 1;
        this.playerIcon = new PlayerAtlasIcon(
                PLAYER_MARKER_TEXTURE,
                PLAYER_MARKER_TEXTURE_SIZE,
                PLAYER_MARKER_TEXTURE_SIZE,
                PLAYER_MARKER_RENDER_SIZE,
                PLAYER_MARKER_RENDER_SIZE
        );
        this.atlasIcons = new ArrayList<>();
        this.atlasIcons.add(this.playerIcon);
        this.waypointIconOptions = createWaypointIconOptions();
        this.selectedWaypointIconIndex = waypointState.selectedWaypointIconIndex;
        this.nextWaypointNumber = waypointState.nextWaypointNumber;

        for (AtlasWaypointStore.WaypointData waypoint : waypointState.waypoints) {
            atlasIcons.add(createWaypointIcon(
                    waypoint.worldX(),
                    waypoint.worldZ(),
                    Component.literal(waypoint.name()),
                    waypoint.iconIndex()
            ));
        }
    }

    @Override
    protected void init() {
        super.init();
        centerOnPlayerPosition();
    }

    private record AtlasViewport(float x, float y, float width, float height, float contentX, float contentY, float contentWidth, float contentHeight) {}

    private AtlasViewport getAtlasViewport() {
        float availableWidth = Math.max(32.0f, this.width - BOOK_SCREEN_MARGIN * 2.0f);
        float availableHeight = Math.max(32.0f, this.height - BOOK_SCREEN_MARGIN * 2.0f);
        float fitScaleX = availableWidth / ATLAS_BACKGROUND_TEXTURE_WIDTH;
        float fitScaleY = availableHeight / ATLAS_BACKGROUND_TEXTURE_HEIGHT;
        float scale = Math.clamp(fitScaleX, 0.25f, Math.min(BOOK_TARGET_UI_SCALE, fitScaleY));

        float width = ATLAS_BACKGROUND_TEXTURE_WIDTH * scale;
        float height = ATLAS_BACKGROUND_TEXTURE_HEIGHT * scale;
        float x = (this.width - width) / 2.0f;
        float y = (this.height - height) / 2.0f;

        float contentX = x + PAGE_AREA_X * scale;
        float contentY = y + PAGE_AREA_Y * scale;
        float contentWidth = PAGE_AREA_WIDTH * scale;
        float contentHeight = PAGE_AREA_HEIGHT * scale;

        return new AtlasViewport(x, y, width, height, contentX, contentY, contentWidth, contentHeight);
    }

    private float getMapOriginX(AtlasViewport viewport, float scaledTileSize) {
        float atlasPixelWidth = atlasWidth * scaledTileSize;
        return viewport.contentX() + (viewport.contentWidth() - atlasPixelWidth) / 2.0f;
    }

    private float getMapOriginY(AtlasViewport viewport, float scaledTileSize) {
        float atlasPixelHeight = atlasHeight * scaledTileSize;
        return viewport.contentY() + (viewport.contentHeight() - atlasPixelHeight) / 2.0f;
    }

    private void centerOnPlayerPosition() {
        centerOnIcon(this.playerIcon);
    }

    private void centerOnIcon(AtlasIcon icon) {
        float scaledTileSize = TILE_SIZE * zoom;
        AtlasViewport viewport = getAtlasViewport();
        Minecraft minecraft = Minecraft.getInstance();

        // Start from neutral pan so anchor is computed in base atlas position
        this.panX = 0;
        this.panY = 0;

        float originX = getMapOriginX(viewport, scaledTileSize);
        float originY = getMapOriginY(viewport, scaledTileSize);

        AtlasIcon.Anchor anchor = icon.resolveAnchor(minecraft, tiles, originX, originY, scaledTileSize);
        if (anchor == null) {
            return;
        }

        this.panX = viewport.contentX() + viewport.contentWidth() / 2.0 - anchor.screenX();
        this.panY = viewport.contentY() + viewport.contentHeight() / 2.0 - anchor.screenY();
    }

    private AtlasIcon createWaypointIcon(double worldX, double worldZ, Component title, int iconIndex) {
        WaypointIconOption option = waypointIconOptions.get(iconIndex);
        return new StaticAtlasIcon(
                option.texture(),
                WAYPOINT_TEXTURE_SIZE,
                WAYPOINT_TEXTURE_SIZE,
                WAYPOINT_RENDER_SIZE,
                WAYPOINT_RENDER_SIZE,
                worldX,
                worldZ,
                title
        );
    }

    private Integer getAtlasScaleFactor() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || tiles.isEmpty()) {
            return null;
        }

        MapItemSavedData firstData = minecraft.level.getMapData(new MapId(tiles.getFirst().mapId()));
        return firstData != null ? (1 << firstData.scale) : null;
    }

    private WorldPoint screenToWorldPoint(double mouseX, double mouseY, float mapOriginX, float mapOriginY, float scaledTileSize) {
        Integer scaleFactor = getAtlasScaleFactor();
        if (scaleFactor == null) {
            return null;
        }

        for (AtlasTilePayload tile : tiles) {
            float tileScreenX = mapOriginX + tile.tileX() * scaledTileSize;
            float tileScreenY = mapOriginY + tile.tileY() * scaledTileSize;

            if (mouseX < tileScreenX || mouseX >= tileScreenX + scaledTileSize || mouseY < tileScreenY || mouseY >= tileScreenY + scaledTileSize) {
                continue;
            }

            double localPixelX = (mouseX - tileScreenX) / (scaledTileSize / 128.0f);
            double localPixelY = (mouseY - tileScreenY) / (scaledTileSize / 128.0f);

            double tileWorldMinX = tile.centerX() - 64.0 * scaleFactor;
            double tileWorldMinZ = tile.centerZ() - 64.0 * scaleFactor;

            double worldX = tileWorldMinX + localPixelX * scaleFactor;
            double worldZ = tileWorldMinZ + localPixelY * scaleFactor;
            return new WorldPoint(worldX, worldZ);
        }

        return null;
    }

    private void cycleSelectedWaypointIcon(int step) {
        if (waypointIconOptions.isEmpty()) {
            return;
        }

        int size = waypointIconOptions.size();
        selectedWaypointIconIndex = Math.floorMod(selectedWaypointIconIndex + step, size);
        waypointState.selectedWaypointIconIndex = selectedWaypointIconIndex;
        if (waypointDraft != null) {
            waypointDraft.iconIndex = selectedWaypointIconIndex;
        }
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

    private void renderAtlasBackground(
            GuiGraphicsExtractor graphics,
            AtlasViewport viewport
    ) {
        int x = (int) Math.floor(viewport.x());
        int y = (int) Math.floor(viewport.y());
        int width = (int) Math.ceil(viewport.width());
        int height = (int) Math.ceil(viewport.height());

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ATLAS_BACKGROUND_TEXTURE,
                x,
                y,
                0.0f,
                0.0f,
                width,
                height,
                ATLAS_BACKGROUND_TEXTURE_WIDTH,
                ATLAS_BACKGROUND_TEXTURE_HEIGHT,
                ATLAS_BACKGROUND_TEXTURE_WIDTH,
                ATLAS_BACKGROUND_TEXTURE_HEIGHT
        );
    }

    private record HoveredAtlasIcon(AtlasIcon icon, AtlasIcon.Anchor anchor, Component title) {}

    private HoveredAtlasIcon findHoveredIcon(
            Minecraft minecraft,
            float mapOriginX,
            float mapOriginY,
            float scaledTileSize,
            int mouseX,
            int mouseY
    ) {
        for (int i = atlasIcons.size() - 1; i >= 0; i--) {
            AtlasIcon icon = atlasIcons.get(i);
            AtlasIcon.Anchor anchor = icon.resolveAnchor(minecraft, tiles, mapOriginX, mapOriginY, scaledTileSize);
            if (anchor == null || !icon.containsPoint(anchor, mouseX, mouseY)) {
                continue;
            }

            Component title = icon.resolveHoverTitle(minecraft);
            if (title != null) {
                return new HoveredAtlasIcon(icon, anchor, title);
            }
        }

        return null;
    }

    private void renderHoveredIconTitle(
            GuiGraphicsExtractor graphics,
            AtlasViewport viewport,
            HoveredAtlasIcon hoveredIcon
    ) {
        int textWidth = this.font.width(hoveredIcon.title());
        int minX = (int) Math.floor(viewport.contentX()) + 2;
        int maxX = (int) Math.ceil(viewport.contentX() + viewport.contentWidth()) - textWidth - 2;
        int centeredX = Mth.floor(hoveredIcon.anchor().screenX() - textWidth / 2.0f);
        int textX = maxX >= minX ? Mth.clamp(centeredX, minX, maxX) : centeredX;

        int minY = (int) Math.floor(viewport.contentY()) + 2;
        int maxY = (int) Math.ceil(viewport.contentY() + viewport.contentHeight()) - this.font.lineHeight - 2;
        int preferredY = Mth.floor(hoveredIcon.anchor().screenY() + hoveredIcon.icon().renderHeight() / 2.0f + ICON_HOVER_TITLE_PADDING);
        int textY = maxY >= minY ? Mth.clamp(preferredY, minY, maxY) : preferredY;

        graphics.textWithBackdrop(this.font, hoveredIcon.title(), textX, textY, textWidth, 0xFFFFFFFF);
    }

    private void renderWaypointDraftOverlay(GuiGraphicsExtractor graphics, AtlasViewport viewport) {
        if (waypointDraft == null || waypointIconOptions.isEmpty()) {
            return;
        }

        WaypointIconOption option = waypointIconOptions.get(waypointDraft.iconIndex);
        String prompt = "Naming waypoint: " + waypointDraft.name + "_";
        String help = "Enter=Save  Esc=Cancel  [/]=Icon  Current=" + option.name();

        int titleX = (int) Math.floor(viewport.contentX()) + 8;
        int titleY = (int) Math.floor(viewport.contentY()) + 8;
        graphics.textWithBackdrop(this.font, Component.literal(prompt), titleX, titleY, this.font.width(prompt), 0xFFFFFFFF);
        graphics.textWithBackdrop(this.font, Component.literal(help), titleX, titleY + this.font.lineHeight + 2, this.font.width(help), 0xFFE0E0E0);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        float scaledTileSize = TILE_SIZE * zoom;
        AtlasViewport viewport = getAtlasViewport();
        Minecraft minecraft = Minecraft.getInstance();

        float originX = getMapOriginX(viewport, scaledTileSize);
        float originY = getMapOriginY(viewport, scaledTileSize);
        float mapOriginX = (float) (originX + panX);
        float mapOriginY = (float) (originY + panY);
        HoveredAtlasIcon hoveredIcon;

        renderAtlasBackground(graphics, viewport);

        int clipX1 = (int) Math.floor(viewport.contentX());
        int clipY1 = (int) Math.floor(viewport.contentY());
        int clipX2 = (int) Math.ceil(viewport.contentX() + viewport.contentWidth());
        int clipY2 = (int) Math.ceil(viewport.contentY() + viewport.contentHeight());
        graphics.enableScissor(clipX1, clipY1, clipX2, clipY2);

        for (AtlasTilePayload tile : tiles) {
            float x = mapOriginX + tile.tileX() * scaledTileSize;
            float y = mapOriginY + tile.tileY() * scaledTileSize;

            renderMapTile(graphics, tile.mapId(), x, y, scaledTileSize / 128.0f);

            boolean hovered =
                    mouseX >= x &&
                            mouseX < x + scaledTileSize &&
                            mouseY >= y &&
                            mouseY < y + scaledTileSize;

            if (hovered) {
                int ix = (int) x;
                int iy = (int) y;
                int isize = (int) Math.ceil(scaledTileSize);

                // subtle translucent white wash
                graphics.fill(ix, iy, ix + isize, iy + isize, 0x22FFFFFF);

                // faint outline
                //graphics.outline(ix, iy, isize, isize, 0x66FFFFFF);
            }
        }

        for (AtlasIcon atlasIcon : atlasIcons) {
            atlasIcon.render(graphics, minecraft, tiles, mapOriginX, mapOriginY, scaledTileSize);
        }

        if (waypointDraft != null && !waypointIconOptions.isEmpty()) {
            Component draftTitle = Component.literal(waypointDraft.name.isBlank() ? "Waypoint" : waypointDraft.name);
            AtlasIcon draftIcon = createWaypointIcon(waypointDraft.worldX, waypointDraft.worldZ, draftTitle, waypointDraft.iconIndex);
            draftIcon.render(graphics, minecraft, tiles, mapOriginX, mapOriginY, scaledTileSize);
        }

        hoveredIcon = findHoveredIcon(minecraft, mapOriginX, mapOriginY, scaledTileSize, mouseX, mouseY);
        if (hoveredIcon != null) {
            renderHoveredIconTitle(graphics, viewport, hoveredIcon);
        }

        renderWaypointDraftOverlay(graphics, viewport);

        graphics.disableScissor();
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            AtlasViewport viewport = getAtlasViewport();
            if (event.x() >= viewport.contentX() && event.x() <= viewport.contentX() + viewport.contentWidth()
                    && event.y() >= viewport.contentY() && event.y() <= viewport.contentY() + viewport.contentHeight()) {
                float scaledTileSize = TILE_SIZE * zoom;
                float originX = getMapOriginX(viewport, scaledTileSize);
                float originY = getMapOriginY(viewport, scaledTileSize);
                float mapOriginX = (float) (originX + panX);
                float mapOriginY = (float) (originY + panY);

                WorldPoint worldPoint = screenToWorldPoint(event.x(), event.y(), mapOriginX, mapOriginY, scaledTileSize);
                if (worldPoint != null && !waypointIconOptions.isEmpty()) {
                    String defaultName = "Waypoint " + nextWaypointNumber;
                    this.waypointDraft = new WaypointDraft(worldPoint.x(), worldPoint.z(), defaultName, selectedWaypointIconIndex);
                    return true;
                }
            }
        }

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
        AtlasViewport viewport = getAtlasViewport();

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

        float oldScaledTileSize = TILE_SIZE * oldZoom;
        float oldOriginX = getMapOriginX(viewport, oldScaledTileSize);
        float oldOriginY = getMapOriginY(viewport, oldScaledTileSize);

        // atlas-space position under cursor before zoom
        double atlasX = (mouseX - oldOriginX - panX) / oldZoom;
        double atlasY = (mouseY - oldOriginY - panY) / oldZoom;

        this.zoom = newZoom;

        float newScaledTileSize = TILE_SIZE * newZoom;
        float newOriginX = getMapOriginX(viewport, newScaledTileSize);
        float newOriginY = getMapOriginY(viewport, newScaledTileSize);

        // keep same atlas-space point under cursor
        this.panX = (float) (mouseX - newOriginX - atlasX * newZoom);
        this.panY = (float) (mouseY - newOriginY - atlasY * newZoom);

        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (waypointDraft != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                waypointDraft = null;
                return true;
            }

            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                String name = waypointDraft.name.trim();
                if (name.isEmpty()) {
                    name = "Waypoint " + nextWaypointNumber;
                }

                atlasIcons.add(createWaypointIcon(waypointDraft.worldX, waypointDraft.worldZ, Component.literal(name), waypointDraft.iconIndex));
                waypointState.waypoints.add(new AtlasWaypointStore.WaypointData(
                        waypointDraft.worldX,
                        waypointDraft.worldZ,
                        name,
                        waypointDraft.iconIndex
                ));
                nextWaypointNumber++;
                waypointState.nextWaypointNumber = nextWaypointNumber;
                selectedWaypointIconIndex = waypointDraft.iconIndex;
                waypointState.selectedWaypointIconIndex = selectedWaypointIconIndex;
                waypointDraft = null;
                return true;
            }

            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!waypointDraft.name.isEmpty()) {
                    waypointDraft.name = waypointDraft.name.substring(0, waypointDraft.name.length() - 1);
                }
                return true;
            }

            if (event.key() == GLFW.GLFW_KEY_LEFT_BRACKET || event.key() == GLFW.GLFW_KEY_COMMA) {
                cycleSelectedWaypointIcon(-1);
                return true;
            }

            if (event.key() == GLFW.GLFW_KEY_RIGHT_BRACKET || event.key() == GLFW.GLFW_KEY_PERIOD || event.key() == GLFW.GLFW_KEY_TAB) {
                cycleSelectedWaypointIcon(1);
                return true;
            }
        }

        if (event.key() == GLFW.GLFW_KEY_R) {
            this.zoom = 1.0f;
            centerOnPlayerPosition();
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
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (waypointDraft != null) {
            if (!event.isAllowedChatCharacter()) {
                return false;
            }

            if (waypointDraft.name.length() >= WAYPOINT_NAME_MAX_LENGTH) {
                return true;
            }

            waypointDraft.name = waypointDraft.name + event.codepointAsString();
            return true;
        }

        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}