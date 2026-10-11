# GhostTracker (Fabric, Minecraft 26.1.2)

Port of [llFireAndIcell/GhostTracker](https://github.com/llFireAndIcell/GhostTracker) (1.8.9 Forge) to Fabric 26.1.2.
Tracks ghost kills, drops (Sorrow, Volta, Plasma, Ghostly Boots, 1m coins), magic find, combat XP, scavenger
coins and profit for Hypixel SkyBlock's Dwarven Mines, with a total tracker HUD and a timed-session HUD.

Licensed GPL-3.0 like the original.

## Building
Requires **JDK 25**.

    gradle wrapper        # once, only if you don't have gradlew (or copy gradlew/gradlew.bat/gradle/ from fabric-example-mod)
    ./gradlew build       # jar ends up in build/libs/GhostTracker-fabric-3.0.0.jar
    ./gradlew runClient   # test in dev

Needs the Fabric API mod at runtime. Versions are in `gradle.properties` (check https://fabricmc.net/develop).

## Commands
- `/ghost` - config screen
- `/ghost start | pause | reset` - timed tracker
- `/ghost stats reset` - reset the total tracker
- `/ghost hud <tracker|timer> toggle | pos <x> <y> | scale <n>`
- `/ghost color <kill|drop|margin|mf|xp|time|coin|pause> <hex>`

Data and settings live in `config/GhostTracker/` (GhostConfig.json, GhostStats.json, GhostTimer.json).

## Differences from the 1.8.9 version
- No OneConfig (it doesn't exist for this version): settings are a button screen + JSON file + commands.
- HUDs are positioned with `/ghost hud ...` instead of dragging. No keybinds yet.
- Chat/action bar matching runs on plain text rather than section-sign formatted text.
- Price fetching is asynchronous (no more client-thread stalls); purse/scavenger use `long`.
- Texture files renamed (no spaces allowed in resource paths) and moved to `textures/gui/`.
- Location detection scans the first 8 sidebar lines instead of a fixed line 5, and now correctly resets when you leave.

## Where to look if an update breaks it
All GUI drawing is in `hud/HudRenderer.java`, the settings screen in `ui/GhostConfigScreen.java`,
sidebar reading in `utils/ScoreboardUtils.java`.

## Building without installing Java
Push this project to a GitHub repo; the workflow in `.github/workflows/build.yml` builds it with JDK 25 and uploads
the jar under Actions -> latest run -> Artifacts. (Use the jar that doesn't end in `-sources`.)
