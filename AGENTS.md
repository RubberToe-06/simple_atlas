# AGENTS.md

## Project Snapshot
- Fabric mod (`simple-atlas`) for Minecraft `26.1`, Java `25`, Loom `1.15.x`.
- Main package: `src/main/java/rubbertoe/simple_atlas`.
- Entrypoints are declared in `src/main/resources/fabric.mod.json` (`main`, `client`, `fabric-datagen`).

## Architecture You Should Learn First
- Server bootstrap: `SimpleAtlas.onInitialize()` wires modules in order: `ModItems`, `ModComponents`, `ModNetworking`, `AtlasViewTicker`.
- Item state is stored in a custom data component: `ModComponents.ATLAS_CONTENTS` with codec in `component/AtlasContents.java`.
- Core gameplay logic lives in `item/AtlasItem.java`:
  - Main-hand use + filled map in offhand -> validate scale, dedupe, append map ID to atlas component.
  - Main-hand use without map (atlas has maps) -> call `AtlasLayoutBuilder.build()`, serialize result as `List<AtlasTilePayload>`, register player with `AtlasViewManager`, send `OpenAtlasScreenPayload` to client.
- Layout computation is in `layout/`: `AtlasLayoutBuilder.build(ServerLevel, AtlasContents)` returns an `AtlasLayout` record; each map becomes an `AtlasMapEntry` with grid coordinates (`tileX`/`tileY`) relative to the origin map. Map span is `128 << scale`.
- Cartography integration is mixin-driven (`mixin/CartographyTableMenuMixin.java`, `CartographyTableAdditionalSlotMixin.java`) rather than vanilla recipe-only behavior. `CartographyTableMenuMixin` also intercepts `quickMoveStack` (shift-click) to route atlas items into slot 1. `mixin/AbstractContainerMenuInvoker.java` exposes `moveItemStackTo` and `broadcastChanges` as `@Invoker` helpers.
- Live map sync is server-managed: `server/AtlasViewManager` tracks active viewers; `AtlasViewTicker` pushes map packets every 10 ticks and registers `ServerPlayConnectionEvents.DISCONNECT` to auto-remove players on disconnect.
- Client UI is `client/screen/AtlasScreen.java` (zoom 0.25–4.0 via scroll, right-drag pan, `R` key resets zoom+pan to player position, hover tile overlay, player marker via `textures/gui/player_marker.png`, close packet on exit). Rendering uses `extractRenderState()` (MC 26.1 API), not `render()`.

## Networking/Data Flow
- Payload types and codecs are in `network/*Payload.java`; registration is centralized in `network/ModNetworking.java`.
- `AtlasTilePayload` is a plain record (mapId, centerX, centerZ, tileX, tileY); its stream codec (`TILE_CODEC`) is defined inside `OpenAtlasScreenPayload` and is **not** registered independently.
- Open flow: `AtlasItem` (server) -> `OpenAtlasScreenPayload` (carries `List<AtlasTilePayload>`) -> `SimpleAtlasClient` receiver -> `AtlasScreen`.
- Close flow: `AtlasScreen.onClose()` sends `CloseAtlasViewPayload` -> server receiver removes player from `AtlasViewManager`.
- If adding a payload, follow existing pattern: define `TYPE` + `CODEC`, register in `ModNetworking`, then wire receiver/sender.

## Developer Workflows (verified tasks)
- Build/test: `./gradlew.bat build` (project currently has no `src/test` sources).
- Run client for manual testing: `./gradlew.bat runClient`.
- Run dedicated server: `./gradlew.bat runServer`.
- Regenerate data assets: `./gradlew.bat runDatagen`.
- Inspect available tasks: `./gradlew.bat tasks --all`.

## Agent Tooling
- `minecraft-dev-mcp` tools are available in this environment; use them to inspect decompiled Minecraft `26.1` classes, method signatures, packets, registries, and mapping names before changing version-sensitive code.
- Prefer checking vanilla call paths and APIs with those tools before editing `mixin/CartographyTableMenuMixin.java`, `client/screen/AtlasScreen.java`, map sync code, or other internals tied closely to Minecraft updates.
- Useful targets to inspect first include `MapItemSavedData`, `CartographyTableMenu`, map packet classes, and rendering classes used by `AtlasScreen.extractRenderState()`.
- If a behavior depends on Minecraft internals, verify the exact `26.1` implementation with `minecraft-dev-mcp` rather than assuming older Yarn/Fabric examples still apply.

## Project-Specific Conventions
- Registry helper pattern: keep registration helpers in module-local classes (example: `item/ModItems.register(...)`).
- Use `Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, ...)` for IDs; avoid hard-coded namespace strings.
- Atlas map IDs preserve insertion order and dedupe (`AtlasContents.withAdded` uses `LinkedHashSet`).
- Treat `src/main/generated` as datagen output; change providers in `datagen/*Provider.java` instead of hand-editing generated JSON.
- Mixin targets use `simple_atlas$` method prefixes for injected/invoker methods.
- Keep side-specific logic separated: client receivers/screens under `client/*`; server state/ticking under `server/*`.
- `debug/AtlasMapDebugEntry.java` has identical fields to `layout/AtlasMapEntry` and appears to be a leftover; prefer `AtlasMapEntry` for any new layout work.

## High-Risk Integration Points
- `CartographyTableMenu` internals are version-sensitive; re-check mixins after Minecraft/Fabric updates.
- Map sync depends on `MapItemSavedData#getUpdatePacket`; null checks are required before sending packets.
- Atlas layout assumes uniform map scale; scale mismatch handling in `AtlasItem` and `CartographyTableMenuMixin` must stay consistent.
- `AtlasScreen.extractRenderState()` uses `GuiGraphicsExtractor` and `MapRenderState` — both are MC 26.1-specific rendering APIs; re-check if the renderer API changes on update.

