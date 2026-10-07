# Zombie mods

Three Fabric mods for Minecraft **26.3**, built from one repository. Each works on its own or together with the others.

| Mod | Folder | Mod ID | Adds |
| --- | --- | --- | --- |
| **Mummy** | `mummy/` | `mummy` | The Mummy: a cute bandage-wrapped desert undead with glowing eyes and loose, cloth-animated bandages. |
| **Verity** | `verity/` | `verity` | Verity: a sunshine-yellow zombie with a big smiley face. |
| **Zombie Kingdom** | `zombie-kingdom/` | `zombie_kingdom` | Abandoned zombie kingdoms with a castle; the Zombie King and Zombie Princess; knights on zombie horses; archers; a stable. |

What each mod does in detail: `curseforge/<mod>/description.md` (the CurseForge page text).

## Mummy

- Kid-friendly look: a cute sage-green face in a bandage hood, bare hands and glowing amber eyes, on the vanilla zombie model (adult and baby).
- Loose bandage strips move like banner cloth: a wave runs down each strip, they hang with gravity from the outstretched arms, trail behind when walking, swing out when turning and lift when falling.
- Zombie stats and behaviour, but doesn't burn in sunlight or turn into a drowned.
- Spawns on the surface in **deserts** (weight 40, groups of 1–3) and from the **Mummy Spawn Egg**.
- Drops rotten flesh, string and the zombie's rare drops. Tagged `#minecraft:zombies` (undead).

## Verity

- A zombie painted sunshine yellow with a smiley face: black oval eyes and a wide grin.
- **1% of naturally spawning zombies** become Verity (`ModEntities.VERITY_CHANCE`). **Verity Spawn Egg**.
- Zombie stats; doesn't burn in sunlight or turn into a drowned. Drops rotten flesh and yellow dye.

## Zombie Kingdom

- **Zombie King** (3D crown, cloth cape) and **Zombie Princess** (little crown, cloth skirt). 1% of natural zombies each. When they meet they bond for life, follow each other like a pet and its owner, and celebrate every reunion with fireworks and hearts. The fireworks hurt players only.
- **Zombie Knights** (iron armour, sword, red shield they raise to block) ride **zombie horses** in red armour; **Zombie Archers** (green hood, bow). Guards defend the royals; royals, guards and zombie horses never fight each other.
- **Kingdoms**: a ruined village inside cobblestone walls with corner towers, a gatehouse and red banners; a four-tower cobblestone castle with a throne room and treasure chest; a stable of tameable zombie horses. One king, one princess, 13 knights (5 mounted) and 11 archers.
- Uncommon: one chance per 40×40-chunk region (`data/zombie_kingdom/worldgen/structure_set/kingdoms.json`), in plains, meadows, forests and savannas (`data/zombie_kingdom/tags/worldgen/biome/has_structure/kingdom.json`), on flat, dry land. `/locate structure zombie_kingdom:kingdom`.
- Spawn eggs: king, princess, knight (arrives mounted), archer.

## Requirements

- Minecraft 26.3, Fabric Loader ≥ 0.19.5, Fabric API
- **JDK 25** to build (for example `brew install --cask temurin@25`)

## Building

```bash
./gradlew build
```

Each mod's jar is in `<mod>/build/libs/<mod>-<version>.jar`; upload that file (not `-sources.jar`) to the mod's CurseForge project. Versions live in `<mod>/gradle.properties`; shared Minecraft, Fabric and Loom versions in the root `gradle.properties`.

Other tasks (add `:mummy:`, `:verity:` or `:zombie-kingdom:` in front to run one mod's):

- `./gradlew :mummy:runClient` starts a dev client with that mod loaded.
- `./gradlew runClientGameTest --no-parallel` launches the game for each mod and runs its checks (stats, spawning, tags, spawn eggs, and for Zombie Kingdom the royal partnership, fireworks, guards, mounted knights, a placed kingdom and one generating naturally), saving screenshots to `<mod>/build/run/clientGameTest/screenshots/`. `--no-parallel` keeps it to one game window at a time.
- `./gradlew runProductionClientGameTest --no-parallel` runs the same checks against the built jars in a production-like game, as a launcher would.

## Textures

Every texture, icon and CurseForge logo is produced by `tools/generate_textures.py` (needs Pillow):

```bash
python3 tools/generate_textures.py
```

The cloth and crown UVs it paints must match the tables in `MummyRenderer`, `ZombieKingRenderer` and `ZombiePrincessRenderer`.

## Layout

| Path | Contents |
| --- | --- |
| `build.gradle`, `gradle.properties` | Shared build for all three mods |
| `<mod>/gradle.properties` | Mod ID, version, jar name |
| `<mod>/src/main` | Entities, registration, mixins, data (loot tables, tags, worldgen) |
| `<mod>/src/client` | Renderers; the cloth animation (`ClothModel`, `ClothStrip`) is copied into Mummy and Zombie Kingdom |
| `<mod>/src/gametest` | Client game tests |
| `curseforge/<mod>/` | CurseForge description and logo |
| `tools/generate_textures.py` | All textures |
