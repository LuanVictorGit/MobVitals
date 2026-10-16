# MobVitals

Real-time packet-based health indicators and cinematic corpse death animations for Minecraft servers running Paper, Spigot, or Folia.

---

## 1. Plugin Overview and Purpose
MobVitals enhances combat and immersion by displaying sleek, real-time health bars above all living entities and players entirely through packet manipulation with **PacketEvents**. When an entity dies, instead of abruptly vanishing into a vanilla puff of smoke, the entity falls sideways onto the ground, remains lying there as a corpse surrounded by thematic ambient particles, and finally dissolves in a cinematic category-specific dissipation effect with custom audio.

All visual indicators and corpse persistence are rendered on the client side without creating additional physical entities on the server.

---

## 2. Current Version
**Version:** `1.0.0`

---

## 3. Project Status
**Status:** `Active / Production Ready`

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
│   │   │   │   └── IndicatorManager.java
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
│           ├── FormatTest.java
│           ├── MetadataTest.java
│           └── PlatformTest.java
```

---

## 9. Implemented Features
1. **Client-Side Packet Indicators:** Virtual ArmorStand holograms spawned exclusively via PacketEvents (`WrapperPlayServerSpawnEntity` and `WrapperPlayServerEntityMetadata`) using negative entity IDs.
2. **Dynamic MiniMessage Health Bars:** Customizable symbols, fill/empty colors, 4 health tiers (High, Medium, Low, Critical), and entity name resolution.
3. **Smooth Position Tracking:** Indicators smoothly follow host entities every tick via `WrapperPlayServerEntityTeleport` and automatically despawn when entities leave view distance or die.
4. **Cinematic Corpse System:** Intercepts `WrapperPlayServerDestroyEntities` to delay despawn for the configured duration (default: 3.5s). The mob stays lying flat on the ground after the vanilla death tilt.
5. **Suppressed Vanilla Death Smoke:** Intercepts entity event status 60 so the vanilla 20-tick smoke poof is hidden.
6. **Subtle Ground-Hugging Ambient Aura:** Rings of subtle particles orbit closely around the corpse, scaled to the mob's bounding box width.
7. **Thematic Death Profiles:** Custom particle orbits, finale dissipation blasts, and audio effects for 11 entity categories (`UNDEAD`, `SKELETAL`, `INFERNAL`, `CREEPER`, `ENDER`, `ARACHNID`, `AQUATIC`, `VILLAGER_ILLAGER`, `BOSS`, `PLAYER`, `ANIMAL`).
8. **Client Visibility Toggle:** Individual players can toggle health indicators on/off with `/mobvitals toggle`.

---

## 10. Commands and Permissions
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/mobvitals toggle` | `mobvitals.use` (default: all) | Toggles personal visibility of health indicators. |
| `/mobvitals reload` | `mobvitals.admin` (default: op) | Reloads `config.yml` settings. |
| `/mobvitals info` | `mobvitals.admin` (default: op) | Displays active indicator count, active corpses, and tracking stats. |
| `/mobvitals test <profile>` | `mobvitals.admin` (default: op) | Spawns preview particles and sounds of a death profile at the player's location. |

Aliases: `/mv`

---

## 11. Configuration Overview
The `config.yml` file contains full English explanations for every option:
- `indicators.enabled`: Global toggle for floating indicators.
- `indicators.view-distance`: Radius in blocks for rendering indicators (default: 32).
- `indicators.height-offset`: Vertical offset above entity head (default: 0.25).
- `indicators.bar-length`: Total segments in the health bar (default: 10).
- `death-animations.corpse-duration-seconds`: Time in seconds corpses remain lying down (default: 3.5).
- `death-animations.ambient-particles`: Toggles the subtle ground orbit aura.
- `death-animations.final-explosion`: Toggles the dissipation effect when the corpse timer expires.
- `entity-filters`: Independent toggles for players, monsters, animals, villagers, and bosses, with an entity type blacklist.

---

## 12. Development and Testing Instructions
1. Run automated unit tests:
   ```bash
   mvn test
   ```
2. Build the JAR:
   ```bash
   mvn clean package
   ```
3. Copy to test server and reload:
   ```bash
   Copy-Item target/MobVitals-1.0.0.jar c:/Users/.../servidor/plugins/ -Force
   # In server console:
   plugman reload MobVitals
   ```
4. Verify console logs for:
   `[MobVitals] PacketEvents detected: Packet-based indicators and corpse visuals activated.`

---

## 13. Known Limitations and Design Decisions
- **ArmorStand Hologram Choice:** Uses virtual small marker ArmorStands for maximum backwards compatibility (1.16.5 through 26.2+). `TEXT_DISPLAY` entities are only supported in 1.19.4+ and would break 1.16.5-1.19.2 support.
- **Authoritative Combat State:** Damage and death events remain strictly managed by Spigot/Paper server events to guarantee data integrity; PacketEvents is used exclusively for client rendering and packet interception.

---

## 14. Important Compatibility Decisions
- **Adventure Component Shading:** Adventure classes (`net.kyori`) are shaded into the JAR *without* relocation patterns, allowing PacketEvents' `EntityDataTypes.OPTIONAL_ADV_COMPONENT` to directly serialize components without class-casting mismatches.
- **Metadata Index Switching:** ArmorStand client flags are dynamically sent to index 14 on Minecraft < 1.17, and index 15 on Minecraft 1.17 - 26.2+.
- **Weak References:** Host entities in `HealthIndicator` are wrapped in `WeakReference<LivingEntity>` to avoid holding stale references across world unloads or chunk resets.

---

## 15. Version History
- **1.0.0 (2026-10-16):**
  - Initial production release.
  - Full PacketEvents virtual health indicators with MiniMessage styling.
  - Lying corpse delay animation with vanilla poof suppression.
  - Entity-category visual/sound profiles.
  - Paper, Spigot, and Folia cross-platform compatibility.

---

## 16. Important Implementation Constraints
Future contributors and AI pair programmers must preserve:
1. **Zero comments in source code:** `.java` files must never contain comments or JavaDoc.
2. **Adventure MiniMessage exclusively:** Never use legacy section `§` or ampersand `&` color formatting.
3. **Folia safety:** Use `Schedulers` abstraction for all tasks and player/entity operations.
4. **Author identity:** Author `lHawk_` and Discord `https://minebuilders.com.br/discord`.
5. **No co-authored trailers:** Git commits must never include `Co-Authored-By` or AI attribution trailers.
