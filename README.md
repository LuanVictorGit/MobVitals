# MobVitals

Real-time packet-based health indicators and cinematic corpse death animations for Minecraft servers running Paper, Spigot, or Folia.

---

## 1. Plugin Overview and Purpose
MobVitals enhances combat and immersion by displaying sleek, real-time health bars above all living entities and players entirely through packet manipulation with **PacketEvents**. When an entity dies, instead of abruptly vanishing into a vanilla puff of smoke, the entity falls sideways onto the ground, remains lying there as a corpse surrounded by thematic ambient particles, and finally dissolves in a cinematic category-specific dissipation effect with custom audio.

All visual indicators and corpse persistence are rendered on the client side without creating additional physical entities on the server.

---

## 2. Current Version
**Version:** `1.0.8`

---

## 3. Project Status
**Status:** `Active / Published on MineBuilders (v1.0.8)`
**MineBuilders Slug:** `mobvitals-health-corpses-1-16-26-3`
**MineBuilders ID:** `cdf014fb-8da6-4fa2-8ab3-7978fe9aea61`

---

## 4. Supported Platforms and Minecraft Versions
- **Platforms:** Paper, Purpur, Spigot, Folia
- **Minecraft Versions:** `1.16.5` through `26.2+`
- **Concurrency Architecture:** Folia region-aware schedulers with automatic Bukkit scheduler fallback.

---

## 5. Required Java Version
- **Runtime:** Java 17 or higher (tested on Java 17, Java 21, and Java 25).
- **Compilation Target:** Java 17 release bytecode.

---

## 6. Build System and Commands
MobVitals uses Apache Maven.

```bash
# Compile and run automated unit tests
mvn clean test

# Build production jar with shaded Adventure dependencies
mvn clean package
```

The compiled artifact is generated at `target/MobVitals-1.0.0.jar`.

---

## 7. Dependencies and Installation
### Required Runtime Dependencies
- **PacketEvents 2.14.0+:** Must be installed in the server `plugins/` directory.

### Shaded Dependencies (included inside jar)
- `net.kyori:adventure-text-minimessage`
- `net.kyori:adventure-text-serializer-legacy`
- `net.kyori:adventure-text-serializer-plain`

### Installation Steps
1. Download and install `packetevents-spigot-2.14.0.jar` into your server's `plugins/` folder.
2. Place `MobVitals-1.0.0.jar` into the `plugins/` folder.
3. Start or reload the server (`plugman reload MobVitals` or restart).
4. Customize options in `plugins/MobVitals/config.yml`.

---

## 8. Project Structure
```
MobVitals/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/me/lhawk/mobvitals/
│   │   │   ├── MobVitalsPlugin.java
│   │   │   ├── command/
│   │   │   │   └── MobVitalsCommand.java
│   │   │   ├── config/
│   │   │   │   └── PluginConfig.java
│   │   │   ├── death/
│   │   │   │   ├── CorpseManager.java
│   │   │   │   └── DeathProfile.java
│   │   │   ├── indicator/
│   │   │   │   ├── HealthIndicator.java
│   │   │   │   ├── IndicatorManager.java
│   │   │   │   └── VariationPopup.java
│   │   │   ├── listener/
│   │   │   │   ├── EntityVitalListener.java
│   │   │   │   └── VitalPacketListener.java
│   │   │   └── util/
│   │   │       ├── Platform.java
│   │   │       ├── Schedulers.java
│   │   │       └── TextUtil.java
│   │   └── resources/
│   │       ├── config.yml
│   │       └── plugin.yml
│   └── test/
│       └── java/me/lhawk/mobvitals/
│           ├── DeathProfileTest.java
│           ├── DenseReductionAndPopupTest.java
│           ├── FormatTest.java
│           ├── MetadataTest.java
│           └── PlatformTest.java
```

---

## 9. Implemented Features
1. **Intangible Packet Visual Engine:** Employs virtual marker ArmorStands (Marker `0x10`, Invisible `0x20`, No Gravity `0x01`) with calibrated eye-height offset (`-1.25 blocks`), rendering crisp vanilla nametag billboards across all Minecraft versions (1.16.5 through 26.2+). Zero hitboxes, zero collisions, zero click/attack interference, and immune to display-entity client culling bugs.
2. **Combat-Only Auto-Hiding Health Bar:** Overhead health indicators remain completely hidden during peaceful exploration and appear only when an entity sustains damage or heals, automatically vanishing after `damaged-display-seconds` (default: 4.0s).
3. **Health Variation Popups:**
   - Real-time floating damage holograms (`-<val> ❤`) with subtle upward drift (`0.035` blocks/tick) and random lateral offsets to prevent visual clutter.
   - Distinct floating heal holograms (`+<val> 💚`) with identical smooth drift.
   - Instant event-driven calculation directly at `MONITOR` priority supporting partial healing, natural regeneration, absorption hearts, and fatal hits.
4. **Strictly Centered Corpse Particles:** Ambient death particles emanate directly from the center of the fallen body `(x, y + bodyCenterY, z)` rather than wide orbital rings, maintaining thematic visual fidelity.
5. **Smart Dense Corpse Reduction:** Dynamically calculates death cluster density within `dense-radius` (default: 4.0m) and scales corpse duration proportionally (from 1.0x down to 0.15x) while accelerating dissolution of older corpses in the pile to eliminate clutter.
6. **Smooth Dissolution Animation:** Expiring corpses burst into thematic finale particles and custom audio before the client destroys the entity, guaranteeing no abrupt visual cuts or lingering ghost models.
7. **Thematic Death Profiles:** Custom particle and sound profiles for 11 entity categories (`UNDEAD`, `SKELETAL`, `INFERNAL`, `CREEPER`, `ENDER`, `ARACHNID`, `AQUATIC`, `VILLAGER_ILLAGER`, `BOSS`, `PLAYER`, `ANIMAL`).
8. **Client Visibility Toggle:** Individual players can toggle health indicators on/off with `/mobvitals toggle`.
9. **One Popup per Hit and Heal:** `variation-popups.max-per-entity` (default `1`) keeps a single damage and a single heal popup above each entity; a new hit or heal replaces the older one.
10. **Plugin Integration:** While ZombieGladiators is installed its gladiators are ignored entirely (bar, popups, corpse), since it draws them itself. Other entities carrying the metadata or scoreboard tag `mobvitals-hide-indicator` get no bar and `mobvitals-hide-popups` no popups; the metadata `mobvitals-name` (MiniMessage) replaces the name in the bar.

---

## 10. Commands and Permissions
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/mobvitals toggle` | `mobvitals.use` (default: all) | Toggles personal visibility of health indicators. |
| `/mobvitals reload` | `mobvitals.admin` (default: op) | Reloads `config.yml` settings. |
| `/mobvitals info` | `mobvitals.admin` (default: op) | Displays active indicator count, active popups, active corpses, and tracking stats. |
| `/mobvitals test <profile>` | `mobvitals.admin` (default: op) | Spawns preview particles and sounds of a death profile at the player's location. |

Aliases: `/mv`, `/vitals`

---

## 11. Configuration Overview
The `config.yml` file contains full English explanations for every option:
- `indicators.enabled`: Global toggle for floating indicators.
- `indicators.show-only-when-damaged`: Auto-hides overhead health bars until damaged/healed.
- `indicators.damaged-display-seconds`: Duration in seconds overhead bars remain visible.
- `indicators.variation-popups`: Floating damage/heal difference popups toggle and formatting.
- `variation-popups.max-per-entity`: Popups of each kind shown at once per entity (default `1`, `0` = no limit).
- `death-animations.dense-corpse-reduction`: Enables smart duration reduction for clustered deaths.
- `death-animations.dense-radius`: Radius in blocks to detect corpse clusters.
- `death-animations.corpse-duration-seconds`: Base time corpses remain lying down.
- `death-animations.ambient-particles`: Ambient body-centered particle aura.
- `death-animations.final-explosion`: Dissolution particle burst.

---

## 12. Development and Testing Instructions
1. Run automated unit tests:
   ```bash
   mvn clean test
   ```
2. Build the shaded production JAR:
   ```bash
   mvn clean package
   ```
3. Deploy to test server:
   ```bash
   Copy-Item target/MobVitals-1.0.0.jar c:/Users/.../servidor/plugins/ -Force
   plugman reload MobVitals
   ```

---

## 13. In-Game Verification and Screenshots
The plugin includes authentic in-game verification captures stored in [`screenshots/`](screenshots/):
- `01_health_indicator_zombie.png`: High-visibility health indicator above hostile mob.
- `02_health_indicator_combat.png`: Real-time health bar tracking during active combat.
- `03_damage_popups.png`: Instant floating damage popups (`-❤`) on normal strikes.
- `04_floating_variation_popups.png`: Consecutive hit variation popups with smooth upward drift.
- `05_corpse_death_animation.png`: Natural sideways falling animation upon lethal blow.
- `06_corpse_ambient_particles.png`: Thematic ambient particle wisps centered on the fallen body.
- `07_corpse_ground_alignment.png`: Ground-anchored corpse position synchronized at 2 ticks and 5 ticks.
- `08_corpse_particle_wisps.png`: Sustained visual effect without red hurt damage overlay.
- `09_corpse_ragdoll_prone.png`: Natural prone death pose preserving entity textures and colors.
- `10_corpse_terrain_stability.png`: Stability and collision isolation on uneven terrain.
- `11_corpse_final_dissipation.png`: Thematic particle explosion upon corpse decay.

---

## 14. Known Limitations and Design Decisions
- **Intangible Marker ArmorStands:** Virtual marker ArmorStands provide universal cross-version nametag rendering across 1.16.5–26.2+. Because Marker (`0x10`) gives a 0x0x0 bounding box, players and projectiles pass right through with zero physical collision.
- **Packet Interception Lifecycle:** When a corpse expires, the delayed destroy flag is removed *prior* to broadcasting the destroy packet, ensuring `VitalPacketListener` does not intercept the genuine cleanup packet.
- **Purely Virtual Entities:** Zero physical entities are ever spawned on the server; all visuals are packet-driven via PacketEvents.

---

## 15. Version History
- **1.0.8 (2026-10-23):**
  - MobVitals detects ZombieGladiators on its own and ignores its gladiators completely (no health bar, popups or corpse animation), with any ZombieGladiators version, on Paper, Spigot, Purpur and Folia. Gladiators are recognised by the persistent `zombiegladiators:gladiator` mark, so nothing is needed from ZombieGladiators.
  - Corpses lie on the ground right below where the mob died: a mob killed mid-air or mid-knockback no longer floats. From the death on, the server's own movement, metadata and death packets for it are dropped for corpse viewers, so nothing moves it again.
  - Corpses keep their natural colors (no more red death tint) and no longer push players: the body uses the sleeping pose with its health kept, which gives it a tiny hitbox, and the dying mob stops colliding on the server during its death animation. The scoreboard team used for this is gone, so it also works on Folia.
  - Folia: health bars now update on their entity's own thread and corpses on their region's thread. Before, Folia threw "Thread failed main thread check", bars vanished right after appearing and corpses broke.
  - A death cancelled by another plugin (Paper) no longer leaves a corpse of a mob that is still alive.
- **1.0.7 (2026-10-23):**
  - One popup per hit and per heal, without clutter: each entity shows at most one damage and one heal popup at a time; a new hit or heal replaces the older one (`variation-popups.max-per-entity`, default `1`, `0` = no limit).
  - ZombieGladiators integration: with both plugins installed every gladiator visual is drawn by one plugin only, ending the duplicated bars and popups. Other plugins (and admins, as scoreboard tags) can hide MobVitals on an entity with `mobvitals-hide-indicator` / `mobvitals-hide-popups`, and name its bar with the `mobvitals-name` metadata.
  - Fixed health bars failing with "Legacy formatting codes have been detected" on Paper 1.21+/26.x for mobs with colored custom names; the name now keeps its colors, and tag-like text in names is shown as typed.
  - Invisible players and mobs, vanished players and spectators no longer get a bar or popups that gave away where they are.
  - Players returning from another world see existing health bars again.
  - Players tracking a dying mob from beyond the corpse view distance now get the normal removal instead of keeping a frozen dead mob on screen.
  - `/mv reload` keeps the players who turned bars off with `/mv toggle`; removed a map that kept the health of every spawned mob forever (memory leak); `/mv test` no longer leaves a task running; the help header shows the real version.
- **1.0.6 (2026-10-23):**
  - Fixed damage/heal popups staying frozen in the air forever when `indicators.enabled` (or the `general.enabled` master switch) was off: popup drift and removal now run on their own, independently of health indicators.
  - Fixed a player with an active health bar changing worlds (portal, teleport, RTP) throwing an error on every tick, which froze every popup on the server while that bar was alive; the bar is now respawned in the new world.
  - One failing health bar or popup no longer stalls the others: the faulty entry is removed and logged to the console.
  - `general.enabled: false` now also turns popups off.
- **1.0.5 (2026-10-17):**
  - Enhanced death animation flow: permitted natural death event packets (status 3) during the initial 2 ticks (~100 ms) so mob bodies naturally tilt and lie flat on the ground.
  - Anchored corpse ground teleportation at 2 ticks with a 5-tick (~250 ms) stability re-confirmation directly at ambient particle coordinates, removing the red damage overlay.
  - Guaranteed instant, reliable hit hologram popups on every damage instance (including lethal hits and consecutive attacks) by dispatching variation popups synchronously on `EntityDamageEvent`.
- **1.0.4 (2026-10-16):**
  - Implemented modern Minecraft 1.21.2+ / 26.2 `WrapperPlayServerEntityPositionSync` (`ClientboundEntityPositionSyncPacket`) support to accurately teleport dead mob bodies directly onto their particle spawn anchors after 1 tick.
  - Zeroed delta movement and ensured `onGround = true` with client-side sleep poses, removing visual desync and residual knockback drift.
- **1.0.3 (2026-10-16):**
  - Fast 1-tick delayed corpse ground positioning and collision isolation using native Bukkit scoreboard team rules.
- **1.0.2 (2026-10-16):**
  - 1-tick damage synchronization for healthbars to match Bukkit final damage.
  - Suppressed permanent red hurt animation on corpse bodies and hidden self-indicators for players.
- **1.0.1 (2026-10-16):**
  - Enhanced healthbar visualization with high-visibility segmented battery bars and MiniMessage color gradient styling.
- **1.0.0 (2026-10-16):**
  - Initial release with PacketEvents real-time health indicators, floating damage/heal popups, and cinematic lying corpse animations.
  - Implemented smart dense corpse reduction and centered corpse ambient particles.
  - Intangible marker armor stand hologram engine with zero collision and instant event-driven damage/heal dispatch.
  - Fixed player infinite death animation loop by isolating player death lifecycle, preventing self-viewing death status packets, and bypassing delayed entity destroy for players.
  - Resolved `NoClassDefFoundError: ComponentSerializer` by shading full Kyori Adventure libraries and utilizing JSON component metadata serialization for PacketEvents compatibility.

---

## 16. Important Implementation Constraints
1. **Zero comments in source code:** `.java` files must never contain comments or JavaDoc.
2. **Adventure MiniMessage exclusively:** Never use legacy section `§` or ampersand `&` color formatting.
3. **Folia safety:** Use `Schedulers` abstraction for all tasks and player/entity operations.
4. **Author identity:** Author `lHawk_` and Discord `https://minebuilders.com.br/discord`.
5. **No co-authored trailers:** Git commits must never include `Co-Authored-By` or AI attribution trailers.
