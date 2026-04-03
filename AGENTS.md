# AGENTS.md

## Project Snapshot
- Fabric mod (`simple-atlas`) for Minecraft `26.1`, Java `25`, Loom `1.15.x`.
- Main package: `src/main/java/rubbertoe/simple_atlas`.
- Entrypoints are declared in `src/main/resources/fabric.mod.json` (`main`, `client`, `fabric-datagen`).
- No existing AI guidance files were found (`README.md`, `AGENT.md`, `.github/copilot-instructions.md`, etc.).

## Architecture You Should Learn First
- Server bootstrap: `SimpleAtlas.onInitialize()` wires modules in order: `ModItems`, `ModComponents`, `ModNetworking`, `AtlasViewTicker`.
- Item state is stored in a custom data component: `ModComponents.ATLAS_CONTENTS` with codec in `component/AtlasContents.java`.
- Core gameplay logic lives in `item/AtlasItem.java`:
  - Main-hand use + filled map in offhand -> validate scale, dedupe, append map ID to atlas component.
  - Main-hand use without map -> build debug layout and open the atlas debug screen.
- Cartography integration is mixin-driven (`mixin/CartographyTableMenuMixin.java`, `CartographyTableAdditionalSlotMixin.java`) rather than vanilla recipe-only behavior.
- Live map sync is server-managed: `server/AtlasViewManager` tracks active viewers; `AtlasViewTicker` pushes map packets every 10 ticks.
- Client UI is `client/screen/AtlasScreen.java` (zoom, pan, hover tile overlay, player marker, close packet on exit).

## Networking/Data Flow
- Payload types and codecs are in `network/*Payload.java`; registration is centralized in `network/ModNetworking.java`.
- Open flow: `AtlasItem` (server) -> `OpenAtlasDebugScreenPayload` -> `SimpleAtlasClient` receiver -> `AtlasScreen`.
- Close flow: `AtlasScreen.onClose()` sends `CloseAtlasViewPayload` -> server receiver removes player from `AtlasViewManager`.
- If adding a payload, follow existing pattern: define `TYPE` + `CODEC`, register in `ModNetworking`, then wire receiver/sender.

## Developer Workflows (verified tasks)
- Build/test: `./gradlew.bat build` (project currently has no `src/test` sources).
- Run client for manual testing: `./gradlew.bat runClient`.
- Run dedicated server: `./gradlew.bat runServer`.
- Regenerate data assets: `./gradlew.bat runDatagen`.
- Inspect available tasks: `./gradlew.bat tasks --all`.

## Project-Specific Conventions
- Registry helper pattern: keep registration helpers in module-local classes (example: `item/ModItems.register(...)`).
- Use `Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, ...)` for IDs; avoid hard-coded namespace strings.
- Atlas map IDs preserve insertion order and dedupe (`AtlasContents.withAdded` uses `LinkedHashSet`).
- Treat `src/main/generated` as datagen output; change providers in `datagen/*Provider.java` instead of hand-editing generated JSON.
- Mixin targets use `simple_atlas$` method prefixes for injected/invoker methods.
- Keep side-specific logic separated: client receivers/screens under `client/*`; server state/ticking under `server/*`.

## High-Risk Integration Points
- `CartographyTableMenu` internals are version-sensitive; re-check mixins after Minecraft/Fabric updates.
- Map sync depends on `MapItemSavedData#getUpdatePacket`; null checks are required before sending packets.
- Atlas layout assumes uniform map scale; scale mismatch handling in `AtlasItem` and `CartographyTableMenuMixin` must stay consistent.

