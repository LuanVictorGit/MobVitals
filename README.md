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

## 13. Known Limitations and Design Decisions
- **Intangible Marker ArmorStands:** Virtual marker ArmorStands provide universal cross-version nametag rendering across 1.16.5–26.2+. Because Marker (`0x10`) gives a 0x0x0 bounding box, players and projectiles pass right through with zero physical collision.
- **Packet Interception Lifecycle:** When a corpse expires, the delayed destroy flag is removed *prior* to broadcasting the destroy packet, ensuring `VitalPacketListener` does not intercept the genuine cleanup packet.
- **Purely Virtual Entities:** Zero physical entities are ever spawned on the server; all visuals are packet-driven via PacketEvents.

---

## 14. Version History
- **1.0.0 (2026-10-16):**
  - Initial release with PacketEvents real-time health indicators, floating damage/heal popups, and cinematic lying corpse animations.
  - Implemented smart dense corpse reduction and centered corpse ambient particles.
  - Intangible marker armor stand hologram engine with zero collision and instant event-driven damage/heal dispatch.
  - Fixed player infinite death animation loop by isolating player death lifecycle, preventing self-viewing death status packets, and bypassing delayed entity destroy for players.
  - Resolved `NoClassDefFoundError: ComponentSerializer` by shading full Kyori Adventure libraries and utilizing JSON component metadata serialization for PacketEvents compatibility.

---

## 15. Important Implementation Constraints
1. **Zero comments in source code:** `.java` files must never contain comments or JavaDoc.
2. **Adventure MiniMessage exclusively:** Never use legacy section `§` or ampersand `&` color formatting.
3. **Folia safety:** Use `Schedulers` abstraction for all tasks and player/entity operations.
4. **Author identity:** Author `lHawk_` and Discord `https://minebuilders.com.br/discord`.
5. **No co-authored trailers:** Git commits must never include `Co-Authored-By` or AI attribution trailers.
