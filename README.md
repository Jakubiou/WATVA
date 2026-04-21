# WATVA

A 2D top-down survival shooter built in Java with Swing. Fight through waves of enemies, unlock abilities, upgrade your stats, and take down bosses to progress through 10 increasingly difficult levels.

---

## Gameplay

- **Move** — WASD or arrow keys
- **Shoot** — hold left mouse button (auto-fire toward cursor)
- **Arc Shield** — right mouse button (blocks incoming projectiles in a semicircle facing your cursor)
- **Dash** — Shift (invincible during dash)
- **Explosion** — Q

Survive each wave, earn coins, spend them on permanent stat upgrades between runs, and pick ability upgrades between waves.

---

## Features

- 10 levels, each with 10 waves
- Two bosses: **Dark Mage** (projectile spiral attacks, meteor strikes, teleport) and **Bunny Boss**
- 8 enemy types: Normal, Giant, Small, Shooting, Slime, Zombie, and the two boss types
- A* pathfinding for enemy navigation
- Permanent upgrades: Damage, HP, Defense, Bullet Speed, Crit Chance, Shield Absorb
- Wave abilities (choose 1 of 3 each wave): Double Shot, Backward Shot, Piercing Arrows, Ricochet, Fire Arrows, Slow Enemies, Speed Boost, Explosion+, Health Regen, Shield
- Crit system: 2–4× damage multiplier with visual burst effect
- Ricochet: arrows bounce off walls up to 2 times
- Arc Shield: right-click force field that absorbs enemy projectiles, upgradeable capacity
- Pet system
- Fullscreen with auto-scaling to any resolution
- Chunk-cached background rendering, frustum culling, spatial grid collision — stable 60 FPS

---

## Requirements

- Java 23 (OpenJDK)
- No external dependencies beyond the JDK

---

## Running

```bash
# Compile
javac -d out -sourcepath src src/Core/Main.java

# Run
java -cp out Core.Main
```

Or open WATVA.jar

