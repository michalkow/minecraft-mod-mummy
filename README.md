# Mummy

A Fabric mod for Minecraft **26.3** that adds the **Mummy**: a bandage-wrapped undead that roams deserts.

- Kid-friendly look: a cute sage-green face peeking out of a bandage hood, bare hands, and glowing amber eyes, on the vanilla zombie model (adult and baby).
- Loose bandage strips hang from the arms, chest, back, hip and hood and move like banner cloth: a wave runs down each strip, they hang with gravity from the outstretched arms, trail behind when walking, swing out when turning and lift when falling.
- Zombie stats: 20 health, 3 attack damage, 2 armor, 0.23 speed, 35 follow range. It also keeps zombie behaviour: reinforcements, baby variants, picking up loot, infecting villagers.
- Unlike a zombie it does not burn in sunlight and does not turn into a drowned underwater.
- Spawns naturally on the surface in **desert** biomes (weight 40, groups of 1–3) and from the **Mummy Spawn Egg** (Spawn Eggs creative tab, next to the husk egg).
- Drops rotten flesh and string (the bandages), plus the zombie's rare drops.
- Tagged `#minecraft:zombies`, so it is undead (Smite, inverted healing/harming).

## Requirements

- Minecraft 26.3, Fabric Loader ≥ 0.19.5, Fabric API
- **JDK 25** to build (for example `brew install --cask temurin@25`)

## Building

```bash
./gradlew build
```

The mod jar is `build/libs/mummy-<version>.jar`; upload that file (not `-sources.jar`) to CurseForge.

Other tasks:

- `./gradlew runClient` starts a dev client with the mod loaded.
- `./gradlew runClientGameTest` launches the game, checks stats, desert spawning, tags and the spawn egg, then saves screenshots to `build/run/clientGameTest/screenshots/`.

## Textures

Every texture is produced by `tools/generate_textures.py` (needs Pillow). Edit the palette or patterns there and rerun:

```bash
python3 tools/generate_textures.py
```

It writes the entity skins (including the bandage-strip UVs, which must match the table in `MummyBandagesModel`), glowing-eye layers, spawn egg, mod icon and `curseforge/logo.png`.

## Layout

| Path | Contents |
| --- | --- |
| `src/main/java/.../entity/Mummy.java` | The entity: a `Zombie` subclass |
| `src/main/java/.../registry/ModEntities.java` | Entity type, attributes, spawn placement, desert spawn |
| `src/main/java/.../registry/ModItems.java` | Spawn egg and creative tab entry |
| `src/client/java/.../render/` | Renderer, glowing-eyes layer, animated bandage strips (`MummyBandagesModel`) |
| `src/main/resources/data/` | Loot table, `#minecraft:zombies` tag |
| `src/gametest/` | Client game test |
| `curseforge/` | Project logo and page description |
