# Simple Atlas

Carry your mapped world in one item.

Simple Atlas adds an **Atlas** item that stores multiple filled maps and opens into a single interactive view, so you can pan around your explored area without juggling lots of maps.

## Features

### Atlas item 

- Craft an Atlas and add filled maps through the cartography table
- Atlas only accepts maps with the same scale as the first map inserted

### Interactive world map

- Scroll to zoom 
- Left-drag to pan
- Press `R` to reset zoom/pan to your current player position
- Player marker is shown directly on the atlas

### Waypoints

- Right-click on the atlas to create waypoints
- Right-click existing waypoint markers for context actions
- User can edit or delete waypoints
- Waypoint names are capped to keep labels readable
- Waypoints are saved in the atlas item itself

### Navigation compass

- Choose **Navigate** on a waypoint to pin the waypoint to the locator bar
- Choose **Stop Navigating** to remove pin
- Multiple waypoints can be pinned

### Multiplayer-aware map syncing

- While a player is holding an atlas, map data is synced live from the server
- Atlas viewers are tracked server-side 

## How to use

1. Craft an Atlas.
2. Put a filled map + atlas into a cartography table to add that map.
3. Hold the atlas in your main hand and use it to open the atlas screen.
4. Scroll/drag to inspect your mapped area.
5. Right-click to manage waypoints and optional navigation.

## Recipe

Shapeless:

- Book
- Feather
- Compass
- Map

## Compatibility

- **Minecraft:** `26.1.x`
- **Loader:** Fabric (`fabric-loader >= 0.18.6`)
- **Java:** `25+`
- **Fabric API:** required

## Notes

- The atlas layout expects maps of the same scale.
- Waypoint and navigation state is tied to each atlas item.
- Designed for both singleplayer and multiplayer survival workflows.

## Issues/Feedback
- Report bugs or suggest improvements on the [GitHub Issues](https://github.com/RubberToe-06/simple_atlas/issues) page

