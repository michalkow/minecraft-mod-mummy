# Mummy

A Fabric mod for Minecraft **26.3** that adds zombie-family mobs: the **Mummy**, a bandage-wrapped undead that roams deserts, the rare zombie royals, the **Zombie King** and the **Zombie Princess**, and **Verity**, a sunshine-yellow smiley zombie.

## Mummy

- Kid-friendly look: a cute sage-green face peeking out of a bandage hood, bare hands, and glowing amber eyes, on the vanilla zombie model (adult and baby).
- Loose bandage strips hang from the arms, chest, back, hip and hood and move like banner cloth: a wave runs down each strip, they hang with gravity from the outstretched arms, trail behind when walking, swing out when turning and lift when falling.
- Zombie stats: 20 health, 3 attack damage, 2 armor, 0.23 speed, 35 follow range. It also keeps zombie behaviour: reinforcements, baby variants, picking up loot, infecting villagers.
- Unlike a zombie it does not burn in sunlight and does not turn into a drowned underwater.
- Spawns naturally on the surface in **desert** biomes (weight 40, groups of 1–3) and from the **Mummy Spawn Egg** (Spawn Eggs creative tab, next to the husk egg).
- Drops rotten flesh and string (the bandages), plus the zombie's rare drops.
- Tagged `#minecraft:zombies`, so it is undead (Smite, inverted healing/harming).

## Zombie King and Zombie Princess

- **Zombie King**: a smiling zombie in royal robes with a 3D gold crown (rubies, sapphires, emeralds) and a red, ermine-trimmed cape that billows like a banner: it trails behind when walking, swings when turning and lifts when falling.
- **Zombie Princess**: an adorable zombie with blonde hair, a little gold crown with a pink jewel, and a pink dress whose flared skirt sways and swings aside for her legs as she walks.
- Both have zombie stats and behaviour, but **don't burn in sunlight** and don't turn into drowned. Their reinforcements are ordinary zombies (their subjects), not more royals.
- **A royal couple:** when a king and a princess meet (within 16 blocks) they become partners for life (saved with the world). They follow each other like a dog follows its owner, teleporting back if left more than 20 blocks behind (fighting still comes first, like a wolf). Every time they come together, on first meeting or after being apart, they spew fireworks and hearts. The fireworks hurt players caught in the blast, but never mobs.
- **Each naturally spawning zombie has a 1% chance to be a Zombie King and a 1% chance to be a Zombie Princess** (`ModEntities.ZOMBIE_KING_CHANCE`, `ZOMBIE_PRINCESS_CHANCE`). Babies happen too.
- **Spawn eggs** for both in the Spawn Eggs tab, next to the zombie egg.
- Drop rotten flesh, gold nuggets (king 1–3, princess 1–2) and the zombie's rare drops.

## Knights and archers

- **Zombie Knight**: full iron armour, iron sword and a red shield with a gold border. When hit it sometimes raises its shield and blocks the next blows until it strikes back. Knights ride **zombie horses** in red leather armour with a saddle (the armour also keeps the horse from burning in the sun): every knight from the spawn egg arrives mounted. If the knight falls, the horse can be tamed.
- **Zombie Archer**: green hood, leather tunic and quiver; shoots arrows like a skeleton.
- Zombie horses count as the kingdom's allies, so guards and their mounts never fight.
- Both are hostile guards: they attack players like zombies, and turn on anything that hurts a nearby king or princess. Royals and guards never fight each other (arrows included). They don't burn in sunlight.
- **Spawn eggs** for both. Otherwise they only appear in kingdoms.
- Knights drop iron nuggets, archers drop arrows (plus the zombie drops and, rarely, their gear).

## Zombie kingdoms

- An abandoned village inside cobblestone castle walls (corner towers, battlements, a gatehouse with a portcullis, red banners), around a big cobblestone castle with four towers. Inside: a throne room with a red carpet, two thrones, a treasure chest and red banners; an upper hall; a walkable roof.
- Home to **one Zombie King and one Zombie Princess** (who bond straight away), **13 knights** (5 of them patrolling the roads on zombie horses) and **11 archers** on the walls, towers and castle roof. Guards stay near the kingdom.
- A **stable** on the east side: four stalls behind fences and closed gates, each with hay, a water trough and an armoured zombie horse you can tame, and a chest with spare saddles, leads and golden carrots.
- Uncommon: one chance per 40×40 chunk region (`data/mummy/worldgen/structure_set/kingdoms.json`), in plains, sunflower plains, meadows, forests, flower and birch forests and savannas (`data/mummy/tags/worldgen/biome/has_structure/kingdom.json`), only on fairly flat, dry land. Find one with `/locate structure mummy:kingdom`.

## Verity

- A zombie painted sunshine yellow from head to toe, with a big smiley face: black oval eyes and a wide grin.
- Zombie stats and behaviour, but it doesn't burn in sunlight and doesn't turn into a drowned.
- **1% of naturally spawning zombies** are Verity (`ModEntities.VERITY_CHANCE`). Babies too.
- **Verity Spawn Egg** in the Spawn Eggs tab.
- Drops rotten flesh, 0–2 yellow dye and the zombie's rare drops.

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
- `./gradlew runClientGameTest` launches the game, checks stats, desert spawning, the zombie-variant rolls, that royals don't burn in sunlight, that a king and princess bond, celebrate and find each other again, that fireworks hurt only players, that knights defend the royals, guard kits, that egg knights ride armoured zombie horses that steer at the player and don't burn, a placed kingdom and its residents (including the mounted patrols and the stable), a kingdom generating by itself in a normal world, tags and all spawn eggs, then saves screenshots to `build/run/clientGameTest/screenshots/`.

## Textures

Every texture is produced by `tools/generate_textures.py` (needs Pillow). Edit the palette or patterns there and rerun:

```bash
python3 tools/generate_textures.py
```

It writes the mummy, Zombie King, Zombie Princess, knight, archer and Verity skins (the bandage, cape, skirt and crown UVs must match the tables in `MummyRenderer`, `ZombieKingRenderer` and `ZombiePrincessRenderer`), glowing-eye layers, spawn egg, mod icon and `curseforge/logo.png`.

## Layout

| Path | Contents |
| --- | --- |
| `src/main/java/.../registry/ModEntities.java` | Entity types, attributes, spawn placement, desert spawn, zombie-to-king roll |
| `src/main/java/.../registry/ModItems.java` | Spawn eggs and creative tab entries |
| `src/main/java/.../entity/` | `Mummy`, `Verity`, `RoyalZombie` (`ZombieKing`, `ZombiePrincess`; partnership and fireworks), `RoyalGuard` (`ZombieKnight`, `ZombieArcher`), `ai/` goals, and `ClothMotion` (smoothed turn/fall motion the cloth reacts to) |
| `src/main/java/.../world/` | The kingdom structure: `KingdomStructure` (where) and `KingdomPiece` (what it builds and who lives there) |
| `src/main/java/.../mixin/` | Natural-spawn hook for the zombie variants, royal reinforcements, fireworks that hurt players only |
| `src/client/java/.../render/` | Renderers; `ClothModel` / `ClothStrip` animate the bandages, cape and skirt; `GuardModel` bow and shield poses; glowing-eyes layer |
| `src/main/resources/data/` | Loot tables, `#minecraft:zombies` tag, kingdom worldgen (structure, structure set, biome tag) |
| `src/gametest/` | Client game test |
| `curseforge/` | Project logo and page description |
