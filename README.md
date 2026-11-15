# PvP Client (Fabric 1.21.4, Java 21)

Dark purple/blue ClickGUI (=), HUD widgets, practice combat modules and a floating Player Info ESP panel.

## Build
1. Install JDK 21.
2. Generate the Gradle wrapper once (needs Gradle 8.12+ installed): `gradle wrapper --gradle-version 8.12`
3. `./gradlew build`  (Windows: `gradlew.bat build`)
4. The mod is `build/libs/pvpclient-1.0.0.jar` (ignore `-sources.jar`).

## Install
Fabric Loader 0.16.10+, Fabric API 0.119.2+1.21.4, then drop the jar into `.minecraft/mods`.

## Run from source
`./gradlew runClient`

## Safety scope
KillAura, TriggerBot, Player ESP and Player Info only run in singleplayer / LAN-host worlds, or on servers
whose host you list under `authorizedServers` in `config/pvpclient.json` (servers you own or administer).
No rotation spoofing, no packet tricks, no hiding from anti-cheat.

## Features added in this version
- **Freecam** (Visuals): free-flying camera, your character stays put. Uses two small mixins (`CameraMixin`, `EntityMixin`).
- **Block Trail** (Visuals): visual-only ghost blocks under your feet; block type, appearance (outline / filled / block model), preview of blocks ahead.
- **Direction Indicators** (HUD): arrows around the crosshair toward nearby players, with distance. Private worlds only.
- **Ender Pearl Trainer** (Combat): predicts pearl landings, shows your own throw prediction, tests aiming at armor-stand dummies (auto aim / auto throw are off by default). Private worlds only.
- **Search bar** in the menu: filters modules and settings across all categories.
