# Changelog

Each release gets a `## <mod_version>` section. The publish workflow uploads that
section as the release notes on Modrinth, CurseForge and GitHub, and fails if it
is missing. Older history is on [Modrinth](https://modrinth.com/mod/simple-atlas/versions).

## 2.0.0

**Breaking:** the mod ID changed from `simple-atlas` to `simple_atlas` to prepare for NeoForge support.
Atlases from existing worlds will not carry over. Your config file is migrated automatically.

- Fixed map markers and the player icon being offset on the atlas when it contains maps from more than one dimension (thanks @lemonpie03)
- Added German translation (thanks @MadByteDE)
- Added Simplified Chinese translation (thanks @AuserTI)

## 1.2.0

Config support!
- Config options for max maps in an atlas, max waypoints, icon sizes, and more

## 1.1.0

Minor texture overhaul
- Credit to PepperoniJabroni for bookmark icon textures
