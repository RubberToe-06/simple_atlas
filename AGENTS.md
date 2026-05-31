# AGENTS.md

## Project Snapshot
- Fabric mod (`simple-atlas`) for Minecraft `26.1`, Java `25`, Loom `1.15-SNAPSHOT`.
- `gradle.properties` currently pins: loader `0.18.6`, Fabric API `0.145.1+26.1`, mod version `1.0.1`.
- Main package: `src/main/java/rubbertoe/simple_atlas`.
- Entrypoints in `src/main/resources/fabric.mod.json`: `main`, `client`, `fabric-datagen`.

## Architecture You Should Learn First
- Server bootstrap (`SimpleAtlas.onInitialize()`) initializes in order: `ModItems`, `ModMapDecorationTypes`, `ModComponents`, `ModNetworking`, `ModCriteria`, `AtlasViewTicker`.
- Atlas state is stored in `ModComponents.ATLAS_CONTENTS` using `component/AtlasContents.java`.
- `AtlasContents` stores map IDs + waypoint state (`waypoints`, `selectedWaypointIconIndex`, `nextWaypointNumber`), caps map count at `256`, sanitizes waypoint names/icon indices/dimensions, and keeps `blankMapCount` as legacy codec compatibility (runtime-normalized to `0`).
- Core gameplay logic is in `item/AtlasItem.java`:
  - `useOn` on banners (main hand only) creates banner-derived waypoints with duplicate-position prevention and `ModNetworking.MAX_WAYPOINT_COUNT` enforcement.
  - `use` builds layout via `AtlasLayoutBuilder.build(...)`, sends `OpenAtlasScreenPayload`, and registers active viewers in `AtlasViewManager`.
  - `inventoryTick` keeps atlas `DataComponents.MAP_ID` synced with current position (`AtlasMapSelector`) and delegates to `Items.FILLED_MAP.inventoryTick(...)` for vanilla marker behavior.
- Layout logic (`layout/AtlasLayoutBuilder.java`) computes `AtlasLayout` from same-scale maps using `128 << scale` span and emits per-tile grid positions.
- Cartography behavior is mixin-driven (`CartographyTableMenuMixin`, `CartographyTableAdditionalSlotMixin`, `CartographyTableMapSlotMixin`, `CartographyTableResultSlotMixin`):
  - Book + atlas: duplicate atlas.
  - Filled map + atlas: add same-scale map after dedupe/limits; lower-scale maps may be integrated through `AtlasCartographyScaler`.
  - Atlas + paper: scale atlas maps by +1.
  - Atlas + atlas: merge map/waypoint contents only when both atlases have equal map counts.
- `CartographyTableMenuMixin` also intercepts `quickMoveStack` so shift-click routes books to slot `0` and atlas items to slot `1` (fallback slot `0` for atlas+atlas merge). `AbstractContainerMenuInvoker` exposes `moveItemStackTo` and `broadcastChanges`.
- `cartography/AtlasCartographyScaler.java` handles atlas-wide scaling and lower-scale integration, dedupes by `(dimension, centerX, centerZ, scale)`, and preserves waypoint metadata.
- `CartographyTableResultSlotMixin` applies server-side post-take effects (book duplication extra copy, scale/integration mutation) and triggers `ModCriteria.ATLAS_CARTOGRAPHY_ACTION`.
- Live map sync:
  - `AtlasViewManager` tracks active viewers.
  - `AtlasViewTicker` pushes updates every 10 ticks for active atlas viewers and closes active view if atlas leaves main hand.
  - `ServerPlayerMixin` intercepts `synchronizeSpecialItemUpdates` for held atlases.
  - Packet augmentation uses `AtlasWaypointDecorations`.
- Client UI is `client/screen/AtlasScreen.java`:
  - Zoom `0.25–4.0`, left-drag pan, `R` reset keybind.
  - Uses `extractRenderState(...)` rendering flow (not `render`).
  - Dimension tabs are built from `AtlasTilePayload.dimension`, default tab follows `OpenAtlasScreenPayload.playerDimension`, player marker only renders on the player’s current dimension tab.
  - Waypoint UI supports create/edit/delete, icon cycling, copy coords, teleport command action, and locator-bar pin/unpin.
  - Right-click map context menu supports map removal request (server-authoritative mutation).
- Client visual smoothing mixin: `mixin/client/ItemInHandRendererNoAtlasReequipMixin.java` prevents atlas hand re-equip animation churn on atlas component updates.

## Networking / Data Flow
- Payload classes are in `network/*Payload.java`; registration/receivers are centralized in `network/ModNetworking.java`.
- `AtlasTilePayload` is a plain record (`mapId`, `centerX`, `centerZ`, `tileX`, `tileY`, `dimension`).
- `OpenAtlasScreenPayload` carries tiles + atlas map IDs + waypoints + selected icon index + next waypoint number + `playerDimension`; tile codec is embedded in that payload.
- Open flow: `AtlasItem.use` -> `OpenAtlasScreenPayload` -> `SimpleAtlasClient` receiver -> `AtlasScreen`.
- Close flow: `AtlasScreen.onClose()` sends `CloseAtlasViewPayload`; server stops viewing and refreshes relevant held-atlas waypoint state.
- Waypoint save flow: `AtlasScreen.persistWaypointState()` -> `SaveAtlasWaypointsPayload` (includes atlas map ID echo) -> server validates atlas identity, sanitizes waypoint list, stores updated `AtlasContents`, reconciles pinned waypoint IDs, and triggers immediate refresh for relevant maps.
- Map removal flow: `AtlasScreen` sends `RemoveAtlasMapPayload` (atlas ID echo + map ID) -> server validates current atlas identity, removes map plus covered waypoints, gives player the removed filled map, reconciles pins, and pushes refresh packets.
- Held-map sync flow: atlas `MAP_ID` selection (`AtlasItem.inventoryTick`) -> `ServerPlayerMixin.synchronizeSpecialItemUpdates` interception -> waypoint decoration augmentation in `AtlasWaypointDecorations`.
- Navigation flow: `NavigateToWaypointPayload` / `UnpinWaypointPayload` / `StopNavigatingPayload` -> server updates `ClientboundTrackedWaypointPacket` pins and performs periodic cleanup when players no longer have an atlas.
- Pin IDs are deterministic from floored waypoint coordinates via `WaypointIconCatalog.navigationWaypointId(...)` (not persisted in `AtlasContents`).

## Developer Workflows
- Build: `./gradlew.bat build`
- Run client: `./gradlew.bat runClient`
- Run dedicated server: `./gradlew.bat runServer`
- Regenerate data assets: `./gradlew.bat runDatagen`
- List tasks: `./gradlew.bat tasks --all`
- Current repo has no `src/test` sources.
- CI reference: `.github/workflows/build.yml` runs `./gradlew build` on Ubuntu `24.04` with Java `25`.

## Agent Tooling Notes
- Use the available `minecraft-dev-*` tools to inspect Minecraft internals (class APIs, packets, registries, mappings) before editing version-sensitive logic.
- Prioritize validating vanilla internals before changing cartography mixins, map packet augmentation, or `AtlasScreen` rendering internals.
- Good first inspection targets: `CartographyTableMenu`, `MapItemSavedData`, map packet types, and rendering APIs used by `AtlasScreen.extractRenderState(...)`.

## Project-Specific Conventions
- Keep registration helpers module-local (`ModItems.register(...)` pattern).
- Use `Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, ...)` for identifiers.
- Atlas map IDs preserve insertion order + dedupe (`LinkedHashSet`).
- Keep waypoint limits aligned: name length `32`, server waypoint cap `256`.
- Keep waypoint icon sets in sync across:
  - `navigation/WaypointIconCatalog.java`
  - `assets/simple-atlas/textures/gui/icons/*.png`
  - `assets/simple-atlas/waypoint_style/*.json`
- Treat `src/main/generated` as datagen output; edit providers under `datagen/*Provider.java` instead of generated JSON.
- Keep mixin helper prefixes as `simple_atlas$...`.
- Keep client/server responsibilities separated (`client/*` vs `server/*`).

## High-Risk Integration Points
- `CartographyTableMenu` internals and inner-slot mixin targets (`$3/$4/$5`) are version-sensitive.
- Map sync depends on `MapItemSavedData#getUpdatePacket`; maintain null-safe behavior and manual refresh fallback semantics where already used.
- `AtlasWaypointDecorations` must preserve vanilla empty-decoration behavior; forcing empty decoration payloads causes flicker/marker clears.
- Atlas layout and cartography logic assume consistent atlas map scale; keep `AtlasItem`, `AtlasLayoutBuilder`, `CartographyTableMenuMixin`, and `AtlasCartographyScaler` behavior aligned.
- `AtlasCartographyScaler` depends on `MapItemSavedData.scaled()`, `createFresh(...)`, and `setColor(...)`; re-check projection/dedupe/edge-shading behavior after MC updates.
- `ServerPlayerMixin` injection target (`ServerPlayer#synchronizeSpecialItemUpdates`) must be re-validated on updates.
- Locator-bar pin cleanup relies on packet updates plus server-side reconciliation in `ModNetworking` (save/removal/inventory-check/disconnect paths).
- Dimension tab correctness depends on `AtlasTilePayload.dimension` and `OpenAtlasScreenPayload.playerDimension`; mismatches break tab selection and player-marker visibility.
- `AtlasScreen.extractRenderState(...)` and related rendering APIs are version-sensitive and should be rechecked after MC/Fabric updates.
