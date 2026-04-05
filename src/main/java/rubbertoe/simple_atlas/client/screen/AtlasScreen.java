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
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import rubbertoe.simple_atlas.SimpleAtlas;
import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.client.screen.icon.AtlasIcon;
import rubbertoe.simple_atlas.client.screen.icon.PlayerAtlasIcon;
import rubbertoe.simple_atlas.client.screen.icon.StaticAtlasIcon;
import rubbertoe.simple_atlas.network.AtlasTilePayload;
import rubbertoe.simple_atlas.network.CloseAtlasViewPayload;
import rubbertoe.simple_atlas.network.NavigateToWaypointPayload;
import rubbertoe.simple_atlas.network.OpenAtlasScreenPayload;
import rubbertoe.simple_atlas.network.SaveAtlasWaypointsPayload;
import rubbertoe.simple_atlas.network.StopNavigatingPayload;

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
    private static final int WAYPOINT_PICKER_PREVIEW_SIZE = 18;
    private static final int WAYPOINT_PICKER_STRIP_ICON_SIZE = 12;
    private static final int WAYPOINT_PICKER_STRIP_VISIBLE = 7;
    private static final int WAYPOINT_PICKER_PADDING = 8;
    private static final int WAYPOINT_PICKER_ARROW_SIZE = 12;
    private static final int WAYPOINT_CONTEXT_MENU_WIDTH = 116;
    private static final int WAYPOINT_CONTEXT_MENU_ROW_HEIGHT = 14;
    private static final int WAYPOINT_CONTEXT_MENU_ROWS = 3;
    private static final int ICON_HOVER_TITLE_PADDING = 4;
    private static final int GRID_DASH_LENGTH = 6;
    private static final int GRID_DASH_GAP = 4;
    private static final int GRID_DASH_COLOR = 0x50D1BFA1;
    private static final int TILE_SIZE = 64;
    private final int atlasWidth;
    private final int atlasHeight;
    private final List<AtlasTilePayload> tiles;
    private final List<Integer> atlasMapIds;
    private final PlayerAtlasIcon playerIcon;
    private final List<AtlasIcon> atlasIcons;
    private final List<AtlasContents.WaypointData> atlasWaypoints;
    private final List<WaypointIconOption> waypointIconOptions;
    private int selectedWaypointIconIndex;
    private int nextWaypointNumber;
    private WaypointDraft waypointDraft;
    private int editingWaypointIndex = -1;
    private int contextMenuWaypointIndex = -1;
    private @Nullable WorldPoint contextMenuWorldPoint;
    private int contextMenuX;
    private int contextMenuY;
    private double panX = 0;
    private double panY = 0;
    private boolean leftDragging = false;
    private float zoom = 2.0f;
    private int closeScreenTicksRemaining = -1;
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float ZOOM_STEP = 1.1f;

    private record WaypointIconOption(String name, Identifier texture) {}

    private static WaypointIconOption createIconOption(String filename) {
        String key = filename.endsWith(".png") ? filename.substring(0, filename.length() - 4) : filename;
        String label = key.replace('_', ' ');
        String[] words = label.split(" ");
        StringBuilder title = new StringBuilder(label.length());
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }

        return new WaypointIconOption(
                title.toString(),
                Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "textures/gui/icons/" + key + ".png")
        );
    }

    private static List<WaypointIconOption> createWaypointIconOptions() {
        return List.of(
                createIconOption("axe.png"),
                createIconOption("black_banner.png"),
                createIconOption("blue_banner.png"),
                createIconOption("brown_banner.png"),
                createIconOption("coal.png"),
                createIconOption("copper_ingot.png"),
                createIconOption("cyan_banner.png"),
                createIconOption("desert_village.png"),
                createIconOption("diamond.png"),
                createIconOption("emerald.png"),
                createIconOption("gold_ingot.png"),
                createIconOption("gray_banner.png"),
                createIconOption("green_banner.png"),
                createIconOption("home.png"),
                createIconOption("iron_ingot.png"),
                createIconOption("jungle_temple.png"),
                createIconOption("lapis_lazuli.png"),
                createIconOption("light_blue_banner.png"),
                createIconOption("light_gray_banner.png"),
                createIconOption("lime_banner.png"),
                createIconOption("magenta_banner.png"),
                createIconOption("nether_portal.png"),
                createIconOption("ocean_monument.png"),
                createIconOption("orange_banner.png"),
                createIconOption("pickaxe.png"),
                createIconOption("pink_banner.png"),
                createIconOption("plains_village.png"),
                createIconOption("purple_banner.png"),
                createIconOption("redstone_dust.png"),
                createIconOption("red_banner.png"),
                createIconOption("red_x.png"),
                createIconOption("savanna_village.png"),
                createIconOption("shovel.png"),
                createIconOption("skull.png"),
                createIconOption("snowy_village.png"),
                createIconOption("swamp_hut.png"),
                createIconOption("sword.png"),
                createIconOption("taiga_village.png"),
                createIconOption("target_point.png"),
                createIconOption("target_x.png"),
                createIconOption("trial_chambers.png"),
                createIconOption("white_banner.png"),
                createIconOption("woodland_mansion.png"),
                createIconOption("yellow_banner.png")
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

    private record WaypointPickerLayout(
            int panelX,
            int panelY,
            int panelX2,
            int panelY2,
            int stripX,
            int stripY,
            int slotWidth,
            int stripHalf,
            int leftArrowX,
            int rightArrowX,
            int arrowY
    ) {}

    public static AtlasScreen fromPayload(OpenAtlasScreenPayload payload) {
        return new AtlasScreen(
                payload.tiles(),
                payload.atlasMapIds(),
                payload.waypoints(),
                payload.selectedWaypointIconIndex(),
                payload.nextWaypointNumber()
        );
    }

    public AtlasScreen(
            List<AtlasTilePayload> tiles,
            List<Integer> atlasMapIds,
            List<AtlasContents.WaypointData> waypoints,
            int selectedWaypointIconIndex,
            int nextWaypointNumber
    ) {
        super(Component.literal("Atlas"));
        this.tiles = tiles;
        this.atlasMapIds = List.copyOf(atlasMapIds);
        this.atlasWaypoints = new ArrayList<>(waypoints);
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
        this.selectedWaypointIconIndex = this.waypointIconOptions.isEmpty()
                ? 0
                : Math.floorMod(selectedWaypointIconIndex, this.waypointIconOptions.size());
        this.nextWaypointNumber = Math.max(1, nextWaypointNumber);

        for (AtlasContents.WaypointData waypoint : this.atlasWaypoints) {
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
        int resolvedIconIndex = waypointIconOptions.isEmpty() ? 0 : Math.floorMod(iconIndex, waypointIconOptions.size());
        WaypointIconOption option = waypointIconOptions.get(resolvedIconIndex);
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
        if (waypointDraft != null) {
            waypointDraft.iconIndex = selectedWaypointIconIndex;
        }
    }

    private void persistWaypointState() {
        ClientPlayNetworking.send(new SaveAtlasWaypointsPayload(
                atlasMapIds,
                atlasWaypoints,
                selectedWaypointIconIndex,
                nextWaypointNumber
        ));
    }

    private boolean isContextMenuOpen() {
        return contextMenuWaypointIndex >= 0 || contextMenuWorldPoint != null;
    }

    private void closeContextMenu() {
        contextMenuWaypointIndex = -1;
        contextMenuWorldPoint = null;
    }

    private int getContextMenuRowCount() {
        return contextMenuWaypointIndex >= 0 ? WAYPOINT_CONTEXT_MENU_ROWS : 1;
    }

    private void openWaypointContextMenu(int waypointIndex, double mouseX, double mouseY) {
        contextMenuWaypointIndex = waypointIndex;
        contextMenuWorldPoint = null;
        int menuHeight = WAYPOINT_CONTEXT_MENU_ROW_HEIGHT * getContextMenuRowCount();
        contextMenuX = Mth.clamp((int) mouseX, 4, Math.max(4, this.width - WAYPOINT_CONTEXT_MENU_WIDTH - 4));
        contextMenuY = Mth.clamp((int) mouseY, 4, Math.max(4, this.height - menuHeight - 4));
    }

    private void openNewWaypointContextMenu(WorldPoint worldPoint, double mouseX, double mouseY) {
        contextMenuWaypointIndex = -1;
        contextMenuWorldPoint = worldPoint;
        contextMenuX = Mth.clamp((int) mouseX, 4, Math.max(4, this.width - WAYPOINT_CONTEXT_MENU_WIDTH - 4));
        contextMenuY = Mth.clamp((int) mouseY, 4, Math.max(4, this.height - WAYPOINT_CONTEXT_MENU_ROW_HEIGHT - 4));
    }

    private int getContextMenuOptionAt(double mouseX, double mouseY) {
        if (!isContextMenuOpen()) {
            return -1;
        }

        int menuHeight = WAYPOINT_CONTEXT_MENU_ROW_HEIGHT * getContextMenuRowCount();
        if (mouseX < contextMenuX || mouseX >= contextMenuX + WAYPOINT_CONTEXT_MENU_WIDTH
                || mouseY < contextMenuY || mouseY >= contextMenuY + menuHeight) {
            return -1;
        }

        return (int) ((mouseY - contextMenuY) / WAYPOINT_CONTEXT_MENU_ROW_HEIGHT);
    }

    private int findHoveredWaypointIndex(
            Minecraft minecraft,
            float mapOriginX,
            float mapOriginY,
            float scaledTileSize,
            int mouseX,
            int mouseY
    ) {
        for (int i = atlasWaypoints.size() - 1; i >= 0; i--) {
            int iconListIndex = i + 1;
            if (iconListIndex >= atlasIcons.size()) {
                continue;
            }

            AtlasIcon icon = atlasIcons.get(iconListIndex);
            AtlasIcon.Anchor anchor = icon.resolveAnchor(minecraft, tiles, mapOriginX, mapOriginY, scaledTileSize);
            if (anchor != null && icon.containsPoint(anchor, mouseX, mouseY)) {
                return i;
            }
        }

        return -1;
    }

    private void beginEditWaypoint(int waypointIndex) {
        if (waypointIndex < 0 || waypointIndex >= atlasWaypoints.size()) {
            return;
        }

        AtlasContents.WaypointData waypoint = atlasWaypoints.get(waypointIndex);
        int iconIndex = waypointIconOptions.isEmpty() ? 0 : Math.floorMod(waypoint.iconIndex(), waypointIconOptions.size());
        this.selectedWaypointIconIndex = iconIndex;
        this.waypointDraft = new WaypointDraft(waypoint.worldX(), waypoint.worldZ(), waypoint.name(), iconIndex);
        this.editingWaypointIndex = waypointIndex;
    }

    private void beginNewWaypoint(WorldPoint worldPoint) {
        String defaultName = "Waypoint " + nextWaypointNumber;
        this.waypointDraft = new WaypointDraft(worldPoint.x(), worldPoint.z(), defaultName, selectedWaypointIconIndex);
        this.editingWaypointIndex = -1;
    }

    private void navigateToWaypoint(int waypointIndex) {
        if (waypointIndex < 0 || waypointIndex >= atlasWaypoints.size()) {
            return;
        }

        AtlasContents.WaypointData waypoint = atlasWaypoints.get(waypointIndex);
        ClientPlayNetworking.send(new NavigateToWaypointPayload(
                waypoint.worldX(),
                waypoint.worldZ(),
                waypoint.name()
        ));

        // Schedule screen close after 2 ticks to allow inventory packet to arrive
        closeScreenTicksRemaining = 2;
    }

    private void stopNavigating() {
        ClientPlayNetworking.send(new StopNavigatingPayload());
        closeScreenTicksRemaining = 2;
    }

    private boolean isWaypointCurrentlyNavigated(int waypointIndex) {
        if (waypointIndex < 0 || waypointIndex >= atlasWaypoints.size()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        ItemStack offhand = minecraft.player.getOffhandItem();
        if (!offhand.is(Items.COMPASS)) {
            return false;
        }

        Component customName = offhand.get(DataComponents.CUSTOM_NAME);
        if (customName == null || !customName.getString().startsWith("Navigate to: ")) {
            return false;
        }

        LodestoneTracker tracker = offhand.get(DataComponents.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty()) {
            return false;
        }

        AtlasContents.WaypointData waypoint = atlasWaypoints.get(waypointIndex);
        BlockPos waypointPos = BlockPos.containing(waypoint.worldX(), 64.0, waypoint.worldZ());
        return tracker.target().get().pos().equals(waypointPos);
    }

    private void deleteWaypoint(int waypointIndex) {
        if (waypointIndex < 0 || waypointIndex >= atlasWaypoints.size()) {
            return;
        }

        atlasWaypoints.remove(waypointIndex);
        atlasIcons.remove(waypointIndex + 1);
        persistWaypointState();
    }

    private void renderWaypointContextMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!isContextMenuOpen()) {
            return;
        }

        int menuHeight = WAYPOINT_CONTEXT_MENU_ROW_HEIGHT * getContextMenuRowCount();
        int menuX2 = contextMenuX + WAYPOINT_CONTEXT_MENU_WIDTH;
        int menuY2 = contextMenuY + menuHeight;
        graphics.fill(contextMenuX, contextMenuY, menuX2, menuY2, 0xE0101010);
        graphics.fill(contextMenuX, contextMenuY, menuX2, contextMenuY + 1, 0xFF707070);
        graphics.fill(contextMenuX, menuY2 - 1, menuX2, menuY2, 0xFF707070);
        graphics.fill(contextMenuX, contextMenuY, contextMenuX + 1, menuY2, 0xFF707070);
        graphics.fill(menuX2 - 1, contextMenuY, menuX2, menuY2, 0xFF707070);

        int hoveredRow = getContextMenuOptionAt(mouseX, mouseY);
        if (hoveredRow >= 0) {
            int rowY1 = contextMenuY + hoveredRow * WAYPOINT_CONTEXT_MENU_ROW_HEIGHT;
            int rowY2 = rowY1 + WAYPOINT_CONTEXT_MENU_ROW_HEIGHT;
            graphics.fill(contextMenuX + 1, rowY1, menuX2 - 1, rowY2, 0x50808080);
        }

        if (contextMenuWaypointIndex >= 0) {
            boolean navigatingThisWaypoint = isWaypointCurrentlyNavigated(contextMenuWaypointIndex);
            String firstAction = navigatingThisWaypoint ? "Stop Navigating" : "Navigate";
            int firstColor = navigatingThisWaypoint ? 0xFFFFB366 : 0xFF8FE0FF;
            graphics.textWithBackdrop(this.font, Component.literal(firstAction), contextMenuX + 6, contextMenuY + 3, this.font.width(firstAction), firstColor);
            graphics.textWithBackdrop(this.font, Component.literal("Edit waypoint"), contextMenuX + 6, contextMenuY + WAYPOINT_CONTEXT_MENU_ROW_HEIGHT + 3, this.font.width("Edit waypoint"), 0xFFFFFFFF);
            graphics.textWithBackdrop(this.font, Component.literal("Delete waypoint"), contextMenuX + 6, contextMenuY + WAYPOINT_CONTEXT_MENU_ROW_HEIGHT * 2 + 3, this.font.width("Delete waypoint"), 0xFFFF8080);
        } else {
            graphics.textWithBackdrop(this.font, Component.literal("New waypoint"), contextMenuX + 6, contextMenuY + 3, this.font.width("New waypoint"), 0xFFFFFFFF);
        }
    }

    private void renderDashedTileGrid(
            GuiGraphicsExtractor graphics,
            AtlasViewport viewport,
            float mapOriginX,
            float mapOriginY,
            float scaledTileSize
    ) {
        if (scaledTileSize <= 1.0f) {
            return;
        }

        int clipX1 = (int) Math.floor(viewport.contentX());
        int clipY1 = (int) Math.floor(viewport.contentY());
        int clipX2 = (int) Math.ceil(viewport.contentX() + viewport.contentWidth());
        int clipY2 = (int) Math.ceil(viewport.contentY() + viewport.contentHeight());

        int mapOriginIntX = Mth.floor(mapOriginX);
        int mapOriginIntY = Mth.floor(mapOriginY);
        float mapOriginFracX = mapOriginX - mapOriginIntX;
        float mapOriginFracY = mapOriginY - mapOriginIntY;
        int dashSpan = GRID_DASH_LENGTH + GRID_DASH_GAP;

        // Preserve fractional panning so the grid slides smoothly instead of pixel-stepping.
        graphics.pose().pushMatrix();
        graphics.pose().translate(mapOriginFracX, mapOriginFracY);

        int firstVertical = (int) Math.floor((clipX1 - mapOriginIntX) / scaledTileSize);
        int lastVertical = (int) Math.ceil((clipX2 - mapOriginIntX) / scaledTileSize);
        int verticalDashStart = clipY1 - Math.floorMod(clipY1 - mapOriginIntY, dashSpan);
        for (int gx = firstVertical; gx <= lastVertical; gx++) {
            int lineX = Mth.floor(mapOriginIntX + gx * scaledTileSize);
            if (lineX < clipX1 || lineX >= clipX2) {
                continue;
            }

            for (int y = verticalDashStart; y < clipY2; y += dashSpan) {
                if (y + GRID_DASH_LENGTH <= clipY1) {
                    continue;
                }
                int y2 = Math.min(clipY2, y + GRID_DASH_LENGTH);
                graphics.fill(lineX, y, lineX + 1, y2, GRID_DASH_COLOR);
            }
        }

        int firstHorizontal = (int) Math.floor((clipY1 - mapOriginIntY) / scaledTileSize);
        int lastHorizontal = (int) Math.ceil((clipY2 - mapOriginIntY) / scaledTileSize);
        int horizontalDashStart = clipX1 - Math.floorMod(clipX1 - mapOriginIntX, dashSpan);
        for (int gy = firstHorizontal; gy <= lastHorizontal; gy++) {
            int lineY = Mth.floor(mapOriginIntY + gy * scaledTileSize);
            if (lineY < clipY1 || lineY >= clipY2) {
                continue;
            }

            for (int x = horizontalDashStart; x < clipX2; x += dashSpan) {
                if (x + GRID_DASH_LENGTH <= clipX1) {
                    continue;
                }
                int x2 = Math.min(clipX2, x + GRID_DASH_LENGTH);
                graphics.fill(x, lineY, x2, lineY + 1, GRID_DASH_COLOR);
            }
        }

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
        int boxPaddingX = 6;
        int boxPaddingY = 3;
        int boxWidth = textWidth + boxPaddingX * 2;
        int boxHeight = this.font.lineHeight + boxPaddingY * 2;

        int minX = (int) Math.floor(viewport.contentX()) + 2;
        int maxX = (int) Math.ceil(viewport.contentX() + viewport.contentWidth()) - boxWidth - 2;
        int centeredX = Mth.floor(hoveredIcon.anchor().screenX() - boxWidth / 2.0f);
        int boxX = maxX >= minX ? Mth.clamp(centeredX, minX, maxX) : centeredX;

        int minY = (int) Math.floor(viewport.contentY()) + 2;
        int maxY = (int) Math.ceil(viewport.contentY() + viewport.contentHeight()) - boxHeight - 2;
        int preferredY = Mth.floor(hoveredIcon.anchor().screenY() + hoveredIcon.icon().renderHeight() / 2.0f + ICON_HOVER_TITLE_PADDING);
        int boxY = maxY >= minY ? Mth.clamp(preferredY, minY, maxY) : preferredY;

        int boxX2 = boxX + boxWidth;
        int boxY2 = boxY + boxHeight;

        graphics.fill(boxX, boxY, boxX2, boxY2, 0xE0101010);
        graphics.fill(boxX, boxY, boxX2, boxY + 1, 0xFF707070);
        graphics.fill(boxX, boxY2 - 1, boxX2, boxY2, 0xFF707070);
        graphics.fill(boxX, boxY, boxX + 1, boxY2, 0xFF707070);
        graphics.fill(boxX2 - 1, boxY, boxX2, boxY2, 0xFF707070);

        int textX = boxX + boxPaddingX;
        int textY = boxY + boxPaddingY;
        graphics.textWithBackdrop(this.font, hoveredIcon.title(), textX, textY, textWidth, 0xFFFFFFFF);
    }

    private void renderWaypointDraftOverlay(GuiGraphicsExtractor graphics, AtlasViewport viewport) {
        if (waypointDraft == null || waypointIconOptions.isEmpty()) {
            return;
        }

        WaypointIconOption option = waypointIconOptions.get(waypointDraft.iconIndex);
        WaypointPickerLayout layout = getWaypointPickerLayout(viewport);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelX2(), layout.panelY2(), 0xB0101010);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelX2(), layout.panelY() + 1, 0xFF606060);
        graphics.fill(layout.panelX(), layout.panelY2() - 1, layout.panelX2(), layout.panelY2(), 0xFF606060);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelX() + 1, layout.panelY2(), 0xFF606060);
        graphics.fill(layout.panelX2() - 1, layout.panelY(), layout.panelX2(), layout.panelY2(), 0xFF606060);

        int previewX = layout.panelX() + 6;
        int previewY = layout.panelY() + 6;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                option.texture(),
                previewX,
                previewY,
                0.0f,
                0.0f,
                WAYPOINT_PICKER_PREVIEW_SIZE,
                WAYPOINT_PICKER_PREVIEW_SIZE,
                WAYPOINT_TEXTURE_SIZE,
                WAYPOINT_TEXTURE_SIZE,
                WAYPOINT_TEXTURE_SIZE,
                WAYPOINT_TEXTURE_SIZE
        );

        String title = option.name() + "  (" + (waypointDraft.iconIndex + 1) + "/" + waypointIconOptions.size() + ")";
        String namePrompt = "Name: " + waypointDraft.name + "_";

        int textX = previewX + WAYPOINT_PICKER_PREVIEW_SIZE + 6;
        graphics.textWithBackdrop(this.font, Component.literal(title), textX, previewY + 2, this.font.width(title), 0xFFFFFFFF);
        graphics.textWithBackdrop(this.font, Component.literal(namePrompt), layout.panelX() + 6, previewY + WAYPOINT_PICKER_PREVIEW_SIZE + 4, this.font.width(namePrompt), 0xFFFFFFFF);

        for (int i = 0; i < WAYPOINT_PICKER_STRIP_VISIBLE; i++) {
            int optionIndex = Math.floorMod(waypointDraft.iconIndex + (i - layout.stripHalf()), waypointIconOptions.size());
            WaypointIconOption stripOption = waypointIconOptions.get(optionIndex);
            int slotX = layout.stripX() + i * layout.slotWidth();

            int slotColor = optionIndex == waypointDraft.iconIndex ? 0xA0FFFFFF : 0x50000000;
            graphics.fill(slotX, layout.stripY(), slotX + WAYPOINT_PICKER_STRIP_ICON_SIZE + 2, layout.stripY() + WAYPOINT_PICKER_STRIP_ICON_SIZE + 2, slotColor);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    stripOption.texture(),
                    slotX + 1,
                    layout.stripY() + 1,
                    0.0f,
                    0.0f,
                    WAYPOINT_PICKER_STRIP_ICON_SIZE,
                    WAYPOINT_PICKER_STRIP_ICON_SIZE,
                    WAYPOINT_TEXTURE_SIZE,
                    WAYPOINT_TEXTURE_SIZE,
                    WAYPOINT_TEXTURE_SIZE,
                    WAYPOINT_TEXTURE_SIZE
            );
        }

        int arrowY2 = layout.arrowY() + WAYPOINT_PICKER_ARROW_SIZE;
        graphics.fill(layout.leftArrowX(), layout.arrowY(), layout.leftArrowX() + WAYPOINT_PICKER_ARROW_SIZE, arrowY2, 0x70000000);
        graphics.fill(layout.rightArrowX(), layout.arrowY(), layout.rightArrowX() + WAYPOINT_PICKER_ARROW_SIZE, arrowY2, 0x70000000);
        graphics.textWithBackdrop(this.font, Component.literal("<"), layout.leftArrowX() + 3, layout.arrowY() + 2, this.font.width("<"), 0xFFFFFFFF);
        graphics.textWithBackdrop(this.font, Component.literal(">"), layout.rightArrowX() + 3, layout.arrowY() + 2, this.font.width(">"), 0xFFFFFFFF);
    }

    private WaypointPickerLayout getWaypointPickerLayout(AtlasViewport viewport) {
        int panelX = (int) Math.floor(viewport.contentX()) + WAYPOINT_PICKER_PADDING;
        int panelY = (int) Math.floor(viewport.contentY()) + WAYPOINT_PICKER_PADDING;
        int panelWidth = (int) Math.floor(viewport.contentWidth()) - WAYPOINT_PICKER_PADDING * 2;
        int panelHeight = WAYPOINT_PICKER_PREVIEW_SIZE + WAYPOINT_PICKER_STRIP_ICON_SIZE + this.font.lineHeight + 18;
        int panelX2 = panelX + panelWidth;
        int panelY2 = panelY + panelHeight;

        int stripY = panelY + 6 + WAYPOINT_PICKER_PREVIEW_SIZE + this.font.lineHeight + 8;
        int slotWidth = WAYPOINT_PICKER_STRIP_ICON_SIZE + 4;
        int stripTotalWidth = WAYPOINT_PICKER_STRIP_VISIBLE * slotWidth;
        int stripX = panelX + Math.max(6, (panelWidth - stripTotalWidth) / 2);
        int stripHalf = WAYPOINT_PICKER_STRIP_VISIBLE / 2;
        int leftArrowX = stripX - WAYPOINT_PICKER_ARROW_SIZE - 4;
        int rightArrowX = stripX + stripTotalWidth + 2;

        return new WaypointPickerLayout(
                panelX,
                panelY,
                panelX2,
                panelY2,
                stripX,
                stripY,
                slotWidth,
                stripHalf,
                leftArrowX,
                rightArrowX,
                stripY
        );
    }

    @Override
    public void tick() {
        super.tick();
        
        if (closeScreenTicksRemaining > 0) {
            closeScreenTicksRemaining--;
            if (closeScreenTicksRemaining == 0) {
                persistWaypointState();
                ClientPlayNetworking.send(new CloseAtlasViewPayload());
                Minecraft.getInstance().setScreen(null);
            }
        }
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

        renderDashedTileGrid(graphics, viewport, mapOriginX, mapOriginY, scaledTileSize);

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

        renderWaypointContextMenu(graphics, mouseX, mouseY);
    }
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (waypointDraft != null) {
            if (event.button() == 0) {
                AtlasViewport viewport = getAtlasViewport();
                WaypointPickerLayout layout = getWaypointPickerLayout(viewport);

                if (event.x() >= layout.leftArrowX() && event.x() < layout.leftArrowX() + WAYPOINT_PICKER_ARROW_SIZE
                        && event.y() >= layout.arrowY() && event.y() < layout.arrowY() + WAYPOINT_PICKER_ARROW_SIZE) {
                    cycleSelectedWaypointIcon(-1);
                    return true;
                }

                if (event.x() >= layout.rightArrowX() && event.x() < layout.rightArrowX() + WAYPOINT_PICKER_ARROW_SIZE
                        && event.y() >= layout.arrowY() && event.y() < layout.arrowY() + WAYPOINT_PICKER_ARROW_SIZE) {
                    cycleSelectedWaypointIcon(1);
                    return true;
                }

                for (int i = 0; i < WAYPOINT_PICKER_STRIP_VISIBLE; i++) {
                    int slotX = layout.stripX() + i * layout.slotWidth();
                    int slotX2 = slotX + WAYPOINT_PICKER_STRIP_ICON_SIZE + 2;
                    int slotY = layout.stripY();
                    int slotY2 = slotY + WAYPOINT_PICKER_STRIP_ICON_SIZE + 2;
                    if (event.x() < slotX || event.x() >= slotX2 || event.y() < slotY || event.y() >= slotY2) {
                        continue;
                    }

                    int optionIndex = Math.floorMod(waypointDraft.iconIndex + (i - layout.stripHalf()), waypointIconOptions.size());
                    waypointDraft.iconIndex = optionIndex;
                    selectedWaypointIconIndex = optionIndex;
                    return true;
                }
            }
        }

        if (isContextMenuOpen()) {
            int option = getContextMenuOptionAt(event.x(), event.y());
            if (event.button() == 0) {
                int waypointIndex = contextMenuWaypointIndex;
                WorldPoint worldPoint = contextMenuWorldPoint;
                closeContextMenu();
                if (waypointIndex >= 0) {
                    if (option == 0) {
                        if (isWaypointCurrentlyNavigated(waypointIndex)) {
                            stopNavigating();
                        } else {
                            navigateToWaypoint(waypointIndex);
                        }
                    } else if (option == 1) {
                        beginEditWaypoint(waypointIndex);
                    } else if (option == 2) {
                        deleteWaypoint(waypointIndex);
                    }
                } else if (option == 0 && worldPoint != null) {
                    beginNewWaypoint(worldPoint);
                }
                return true;
            }

            if (event.button() == 1) {
                closeContextMenu();
                if (option >= 0) {
                    return true;
                }
            }
        }

        if (event.button() == 1) {
            AtlasViewport viewport = getAtlasViewport();
            if (event.x() >= viewport.contentX() && event.x() <= viewport.contentX() + viewport.contentWidth()
                    && event.y() >= viewport.contentY() && event.y() <= viewport.contentY() + viewport.contentHeight()) {
                float scaledTileSize = TILE_SIZE * zoom;
                float originX = getMapOriginX(viewport, scaledTileSize);
                float originY = getMapOriginY(viewport, scaledTileSize);
                float mapOriginX = (float) (originX + panX);
                float mapOriginY = (float) (originY + panY);
                int hoveredWaypointIndex = findHoveredWaypointIndex(
                        Minecraft.getInstance(),
                        mapOriginX,
                        mapOriginY,
                        scaledTileSize,
                        (int) event.x(),
                        (int) event.y()
                );

                if (hoveredWaypointIndex >= 0) {
                    openWaypointContextMenu(hoveredWaypointIndex, event.x(), event.y());
                    leftDragging = false;
                    return true;
                }

                WorldPoint worldPoint = screenToWorldPoint(event.x(), event.y(), mapOriginX, mapOriginY, scaledTileSize);
                if (worldPoint != null && !waypointIconOptions.isEmpty()) {
                    openNewWaypointContextMenu(worldPoint, event.x(), event.y());
                    leftDragging = false;
                    return true;
                }
            }

            return false;
        }

        if (event.button() == 0 && waypointDraft == null && !isContextMenuOpen()) {
            AtlasViewport viewport = getAtlasViewport();
            if (event.x() >= viewport.contentX() && event.x() <= viewport.contentX() + viewport.contentWidth()
                    && event.y() >= viewport.contentY() && event.y() <= viewport.contentY() + viewport.contentHeight()) {
                leftDragging = true;
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (leftDragging && event.button() == 0) {
            panX += dx;
            panY += dy;
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            leftDragging = false;
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
                editingWaypointIndex = -1;
                return true;
            }

            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                String name = waypointDraft.name.trim();
                if (name.isEmpty()) {
                    name = "Waypoint " + nextWaypointNumber;
                }

                AtlasContents.WaypointData updatedWaypoint = new AtlasContents.WaypointData(
                        waypointDraft.worldX,
                        waypointDraft.worldZ,
                        name,
                        waypointDraft.iconIndex
                );

                if (editingWaypointIndex >= 0 && editingWaypointIndex < atlasWaypoints.size()) {
                    atlasWaypoints.set(editingWaypointIndex, updatedWaypoint);
                    atlasIcons.set(editingWaypointIndex + 1, createWaypointIcon(
                            updatedWaypoint.worldX(),
                            updatedWaypoint.worldZ(),
                            Component.literal(updatedWaypoint.name()),
                            updatedWaypoint.iconIndex()
                    ));
                } else {
                    atlasWaypoints.add(updatedWaypoint);
                    atlasIcons.add(createWaypointIcon(
                            updatedWaypoint.worldX(),
                            updatedWaypoint.worldZ(),
                            Component.literal(updatedWaypoint.name()),
                            updatedWaypoint.iconIndex()
                    ));
                    nextWaypointNumber++;
                }

                selectedWaypointIconIndex = waypointDraft.iconIndex;
                waypointDraft = null;
                editingWaypointIndex = -1;
                persistWaypointState();
                return true;
            }

            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!waypointDraft.name.isEmpty()) {
                    waypointDraft.name = waypointDraft.name.substring(0, waypointDraft.name.length() - 1);
                }
                return true;
            }

            // While drafting, consume all other keys so typing only affects the name field.
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE && isContextMenuOpen()) {
            closeContextMenu();
            return true;
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
        persistWaypointState();
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







