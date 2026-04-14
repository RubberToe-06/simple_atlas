# AGENTS.md

## Project Snapshot
- Fabric mod (`simple-atlas`) for Minecraft `26.1` (year.update format: `26.1` = 2026 update 1), Java `25`, Loom `1.15.x`.
- Minecraft versions now use a year.update scheme; treat version bumps as update-track changes within that yearly line.
- Main package: `src/main/java/rubbertoe/simple_atlas`.
- Entrypoints are declared in `src/main/resources/fabric.mod.json` (`main`, `client`, `fabric-datagen`).

## Architecture You Should Learn First
- Server bootstrap: `SimpleAtlas.onInitialize()` wires modules in order: `ModItems`, `ModMapDecorationTypes`, `ModComponents`, `ModNetworking`, `AtlasViewTicker`.
- Item state is stored in a custom data component: `ModComponents.ATLAS_CONTENTS` with codec in `component/AtlasContents.java`.
- `AtlasContents` stores atlas map IDs and waypoint state (`waypoints`, `selectedWaypointIconIndex`, `nextWaypointNumber`) and sanitizes waypoint names/icon indices; `blankMapCount` remains codec-only legacy compatibility and is normalized to `0` at runtime.
- Core gameplay logic lives in `item/AtlasItem.java`:
  - Main-hand `useOn` on a banner appends a waypoint using banner color/name (`WaypointIconCatalog.bannerIconIndexForColor(...)`), with duplicate-position rejection and `ModNetworking.MAX_WAYPOINT_COUNT` enforcement.
  - Main-hand use with stored map IDs -> call `AtlasLayoutBuilder.build()`, serialize result as `List<AtlasTilePayload>`, register player with `AtlasViewManager`, send `OpenAtlasScreenPayload` to client.
  - `inventoryTick(...)` continuously picks the current map via `AtlasMapSelector`, writes `DataComponents.MAP_ID`, then delegates to `Items.FILLED_MAP.inventoryTick(...)` so held atlas maps keep vanilla player-marker behavior.
- Layout computation is in `layout/`: `AtlasLayoutBuilder.build(ServerLevel, AtlasContents)` returns an `AtlasLayout` record; each map becomes an `AtlasMapEntry` with grid coordinates (`tileX`/`tileY`) relative to the origin map. Map span is `128 << scale`.
- Cartography integration is mixin-driven (`mixin/CartographyTableMenuMixin.java`, `CartographyTableAdditionalSlotMixin.java`, `CartographyTableMapSlotMixin.java`, `CartographyTableResultSlotMixin.java`) rather than vanilla recipe-only behavior.
- Cartography recipes handled in `CartographyTableMenuMixin`: book + atlas duplicates atlas, filled map + atlas appends map ID after dedupe/scale checks, atlas + atlas merges map/waypoint contents (only when both atlases have equal map-count size).
- `CartographyTableMenuMixin` intercepts `quickMoveStack` (shift-click) to route books into slot 0 and atlas items into slot 1 (fallback slot 0 for atlas+atlas merge). `mixin/AbstractContainerMenuInvoker.java` exposes `moveItemStackTo` and `broadcastChanges` as `@Invoker` helpers.
- `CartographyTableResultSlotMixin` augments result `onTake` for book duplication by granting the second atlas copy.
- Live map sync is server-managed: `server/AtlasViewManager` tracks active viewers; `AtlasViewTicker` pushes map packets every 10 ticks for active viewers; `mixin/ServerPlayerMixin.java` augments normal held-item map sync (`synchronizeSpecialItemUpdates`) for atlas stacks; both paths apply `AtlasWaypointDecorations` before sending.
- `AtlasViewTicker` closes active atlas views when atlas leaves main hand and, while viewing, syncs current atlas-map updates for atlas stacks in either hand.
- Client UI is `client/screen/AtlasScreen.java` (zoom 0.25–4.0 via scroll, left-drag pan, default `R` keybind resets zoom+pan to player position, hover tile overlay, player marker via `textures/gui/player_marker.png`, close packet on exit). Rendering uses `extractRenderState()` (MC 26.1 API), not `render()`.
- `AtlasScreen` also manages waypoint UX (right-click context menu, create/edit/delete, icon picker, name entry, waypoint hover titles, copy coordinates, and pinned-marker rendering) and can pin/unpin waypoints to the vanilla locator bar.
- `AtlasScreen` context menus also include a local teleport action (`tp <x> ~ <z>`) for both waypoint and map-point entries.
- Locator-bar pinning is player-local and temporary: `network/ModNetworking.java` tracks pinned waypoint UUIDs per player, sends `ClientboundTrackedWaypointPacket` updates, and clears pins when the player no longer has any atlas in inventory.

## Networking/Data Flow
- Payload types and codecs are in `network/*Payload.java`; registration is centralized in `network/ModNetworking.java`.
- `AtlasTilePayload` is a plain record (mapId, centerX, centerZ, tileX, tileY); its stream codec (`TILE_CODEC`) is defined inside `OpenAtlasScreenPayload` and is **not** registered independently.
- Open flow: `AtlasItem` (server) -> `OpenAtlasScreenPayload` (carries `List<AtlasTilePayload>`) -> `SimpleAtlasClient` receiver -> `AtlasScreen`.
- Close flow: `AtlasScreen.onClose()` sends `CloseAtlasViewPayload` -> server receiver removes player from `AtlasViewManager`.
- Waypoint persistence flow: `AtlasScreen.persistWaypointState()` -> `SaveAtlasWaypointsPayload` (includes `atlasMapIds` echo) -> server validates current atlas identity, sanitizes waypoint list, writes updated `AtlasContents`.
- Held-map sync flow: `AtlasItem.inventoryTick(...)` updates atlas `DataComponents.MAP_ID` -> `ServerPlayerMixin.synchronizeSpecialItemUpdates(...)` intercepts atlas updates -> `AtlasWaypointDecorations.withAtlasWaypointDecorations(...)` merges atlas waypoint decorations into `ClientboundMapItemDataPacket` when decoration payload is present.
- Navigation flow: `AtlasScreen` context menu -> `NavigateToWaypointPayload` / `UnpinWaypointPayload` -> server sends `ClientboundTrackedWaypointPacket` add/remove updates for locator-bar pins. `StopNavigatingPayload` remains an internal clear-all path.
- Pin identity is derived from floored waypoint coordinates via `navigation/WaypointIconCatalog.navigationWaypointId(...)`, not stored in `AtlasContents`.
- If adding a payload, follow existing pattern: define `TYPE` + `CODEC`, register in `ModNetworking`, then wire receiver/sender.

## Developer Workflows (verified tasks)
- Build/test: `./gradlew.bat build` (project currently has no `src/test` sources).
- Run client for manual testing: `./gradlew.bat runClient`.
- Run dedicated server: `./gradlew.bat runServer`.
- Regenerate data assets: `./gradlew.bat runDatagen`.
- Inspect available tasks: `./gradlew.bat tasks --all`.

## Agent Tooling
- `minecraft-dev-mcp` tools are available in this environment; use them to inspect decompiled Minecraft `26.1` classes, method signatures, packets, and registries before changing version-sensitive code.
- Prefer checking vanilla call paths and APIs with those tools before editing `mixin/CartographyTableMenuMixin.java`, `client/screen/AtlasScreen.java`, map sync code, or other internals tied closely to Minecraft updates.
- Useful targets to inspect first include `MapItemSavedData`, `CartographyTableMenu`, map packet classes, and rendering classes used by `AtlasScreen.extractRenderState()`.
- If a behavior depends on Minecraft internals, verify the exact `26.1` implementation with `minecraft-dev-mcp` rather than assuming older mapping-era Fabric examples still apply.

## Project-Specific Conventions
- Registry helper pattern: keep registration helpers in module-local classes (example: `item/ModItems.register(...)`).
- Use `Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, ...)` for IDs; avoid hard-coded namespace strings.
- Minecraft source for current versions is unobfuscated; use official Minecraft names directly (no Yarn remapping layer needed).
- Atlas map IDs preserve insertion order and dedupe (`AtlasContents.withAdded` uses `LinkedHashSet`).
- Treat `blankMapCount` in `AtlasContents` as legacy-read compatibility only; do not build new behavior around it unless a migration explicitly reintroduces runtime usage.
- Waypoint names are capped at 32 chars and icon indices are clamped/sanitized in `AtlasContents.WaypointData`; keep client and server limits aligned.
- Waypoint list writes are capped server-side at 256 entries (`ModNetworking.MAX_WAYPOINT_COUNT`); preserve that ceiling when changing waypoint save flows.
- Keep waypoint icon key sets aligned across `navigation/WaypointIconCatalog.java`, `assets/simple-atlas/textures/gui/icons/*.png`, and `assets/simple-atlas/waypoint_style/*.json`.
- Treat `src/main/generated` as datagen output; change providers in `datagen/*Provider.java` instead of hand-editing generated JSON.
- Mixin targets use `simple_atlas$` method prefixes for injected/invoker methods.
- Keep side-specific logic separated: client receivers/screens under `client/*`; server state/ticking under `server/*`.

## High-Risk Integration Points
- `CartographyTableMenu` internals are version-sensitive; re-check mixins after Minecraft/Fabric updates.
- Map sync depends on `MapItemSavedData#getUpdatePacket`; null checks are required before sending packets.
- Waypoint decoration augmentation must preserve vanilla packet behavior when `ClientboundMapItemDataPacket.decorations()` is empty; forcing `Optional.of(emptyList())` causes marker flicker/clears.
- Atlas layout assumes uniform map scale; scale mismatch handling in `AtlasItem` and `CartographyTableMenuMixin` must stay consistent.
- Cartography mixins target inner slot classes (`CartographyTableMenu$3/$4/$5`); re-validate target names and `onTake` behavior after Minecraft updates.
- `ServerPlayerMixin` targets `ServerPlayer#synchronizeSpecialItemUpdates`; re-check this injection point and signature on Minecraft updates.
- Locator-bar pin cleanup depends on `ClientboundTrackedWaypointPacket` updates plus server-side pin reconciliation in `ModNetworking` (save, atlas-loss tick cleanup, disconnect); re-check if tracked waypoint internals change.
- `AtlasScreen.extractRenderState()` uses `GuiGraphicsExtractor` and `MapRenderState` - both are MC `26.1` (2026 update 1)-specific rendering APIs; re-check if the renderer API changes on update.
