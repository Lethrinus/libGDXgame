# libGDXgame
***SEN3006 - Software Architecture***

Knight’s Crusade is a top-down action game made with libGDX and LWJGL3. You play as a lone knight defending a village against ever-stronger waves of goblins wielding clubs, goblins throwing dynamite, and barrel bombers that detonate on impact. Each run drops you onto a freshly generated map with enemies spawning from different points. As you fight, you collect gold to spend between waves on boosting your health, strength, and speed. Mastering movement, timing your attacks, and dodging incoming blasts is key, since each wave grows more challenging. Simple controls and a straightforward upgrade system make the game easy to start while still offering a deep, satisfying challenge as you push your skills to the limit.

## Table of Contents

1. [Features](#features)
2. [Gameplay Video](#gameplay-video)  
3. [Requirements](#requirements)  
4. [Getting Started](#getting-started)  
5. [Running the Game](#running-the-game)  
6. [Controls](#controls)  
7. [Architecture](#architecture)  
8. [Asset Pipeline](#asset-pipeline)
9. [Project Report](#project-report)
10. [Authors & License](#authors--license)

## Features

- **Multiple enemy types** with unique behaviours (goblins, dynamite goblins, barrel bombers)  
- **A\* pathfinding** and crowd separation (boids-style) for smooth group movement  
- **Health bars**, knockback, and particle-style effects  
- **NPC dialogue system** with typing effect and wave prompts  
- **Waves & spawn manager**, with upgrade menu between waves  
- **Shaders** for circle-fade foliage and pause-mode greyscale  
- **Ghost-trail dash** for the player, plus inventory and consumable items


## Gameplay Video 

[Watch the gameplay on YouTube](https://youtu.be/T5TjWVjfiRQ)


# Requirements

- **Java 17** JDK (or newer)  
- **Gradle 8+**  
- **libGDX 1.13.1** (desktop LWJGL3 backend)  
- **Optional:** GraalVM native build tools (if you enable `enableGraalNative`)

## Getting Started

1. Clone the repository:  
   ```bash
   git clone https://github.com/lethrinus/libGDXgame.git
   cd libGDXgame

## Running the Game

### From your IDE
1. Import the **lwjgl3** (desktop) module into your IDE as a Gradle project.  
2. In the **lwjgl3** module, locate and run the `Lwjgl3Launcher` main class.

### From the command line
```bash
# In the repo root:
./gradlew desktop:run
```

## Controls

| Action                | Key(s)            |
|-----------------------|-------------------|
| Move                  | W / A / S / D     |
| Attack                | Left mouse button |
| Dash                  | F                 |
| Switch inventory slot | Mouse wheel       |
| Use selected item     | R                 |
| Interact with NPC     | E                 |
| Pause / Resume        | ESC               |


 ## Architecture

The code follows a component-style, event-driven design:

- **CoreGame**  
  Orchestrates game loop, rendering, input, camera, UI, pause, waves, and global event bus.

- **Entities**  
  - **Player**, **Goblin**, **DynamiteGoblin**, **BarrelBomber**, **NPC**, **GoldBag**  
  - Inherit from `BaseEnemy` or implement custom behaviour.
  - Use A\* (`GridPathfinder`), separation, and state machines.

- **Systems & Managers**  
  - **WaveManager**: controls wave spawning, countdowns, and transitions.  
  - **SpawnManager**: random spawn-point provider.  
  - **TileMapRenderer**: loads Tiled map, handles collision layers, shaders.  
  - **CameraShake**: screen-shake effect.  
  - **ShaderManager**: builds and caches custom GLSL shaders.

- **UI & Overlays**  
  - **InventoryHUD**, **GoldCounterUI**, **WaveCounterUI**, **UpgradeMenu**, **PauseManager**, **CountdownOverlay**  
  - Scene2D for menus and in-game overlays.

- **Design Patterns**  
  - **Singleton** for `EventBus` (global pub/sub).  
  - **Factory** (`GameEntityFactory`) to instantiate all entities.  
  - **Observer** via `GameEventListener` for decoupled event handling.  
  - **Strategy / State Machine** for enemy behaviors (MOVE / ATTACK / DIE states).
 
  ## Asset Pipeline

- **Tiled** (.tmx) for map design  
- **libGDX TexturePacker** for sprite atlases  
- **itch.io “Tiny Swords”** asset pack ([link](https://pixelfrog-assets.itch.io/tiny-swords))


## Authors & License

**Authors** 

- Yavuzhan Özbek (2201927) 
- Ozan Halis Demiralp (2203046) 

**License**  
This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

##  Project Report

You can read the detailed project report here: [Project Report.pdf](./Project%20Report.pdf)


