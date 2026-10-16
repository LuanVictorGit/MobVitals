package me.lhawk.mobvitals.death;

import org.bukkit.Sound;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.Locale;

public enum DeathProfile {
    UNDEAD(
            new String[]{"SOUL_FIRE_FLAME", "SMOKE"},
            new String[]{"LARGE_SMOKE", "SOUL", "ASH"},
            Sound.ENTITY_ZOMBIE_AMBIENT,
            Sound.ENTITY_ZOMBIE_DEATH,
            0.6f, 0.8f
    ),
    SKELETAL(
            new String[]{"WHITE_SMOKE", "ASH"},
            new String[]{"CLOUD", "CRIT"},
            Sound.ENTITY_SKELETON_STEP,
            Sound.ENTITY_SKELETON_DEATH,
            0.8f, 0.9f
    ),
    INFERNAL(
            new String[]{"FLAME", "LAVA", "DRIPPING_LAVA"},
            new String[]{"EXPLOSION", "LAVA", "FLAME"},
            Sound.BLOCK_FIRE_AMBIENT,
            Sound.ENTITY_BLAZE_DEATH,
            0.9f, 1.0f
    ),
    CREEPER(
            new String[]{"WHITE_SMOKE", "ITEM_SLIME"},
            new String[]{"EXPLOSION", "ITEM_SLIME"},
            Sound.ENTITY_CREEPER_PRIMED,
            Sound.ENTITY_GENERIC_EXPLODE,
            0.5f, 1.0f
    ),
    ENDER(
            new String[]{"PORTAL", "DRAGON_BREATH"},
            new String[]{"DRAGON_BREATH", "PORTAL", "FLASH"},
            Sound.ENTITY_ENDERMAN_AMBIENT,
            Sound.ENTITY_ENDERMAN_TELEPORT,
            0.7f, 1.0f
    ),
    ARACHNID(
            new String[]{"ITEM_SLIME", "WHITE_SMOKE"},
            new String[]{"CLOUD", "ITEM_SLIME"},
            Sound.ENTITY_SPIDER_AMBIENT,
            Sound.ENTITY_SPIDER_DEATH,
            0.8f, 0.9f
    ),
    AQUATIC(
            new String[]{"BUBBLE", "SPLASH"},
            new String[]{"SPLASH", "BUBBLE"},
            Sound.ENTITY_DOLPHIN_AMBIENT_WATER,
            Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED,
            0.8f, 1.0f
    ),
    VILLAGER_ILLAGER(
            new String[]{"HAPPY_VILLAGER", "ENCHANT"},
            new String[]{"HAPPY_VILLAGER", "END_ROD", "FLASH"},
            Sound.BLOCK_NOTE_BLOCK_CHIME,
            Sound.BLOCK_BELL_RESONATE,
            0.8f, 1.0f
    ),
    BOSS(
            new String[]{"SONIC_BOOM", "SOUL"},
            new String[]{"EXPLOSION_EMITTER", "FLASH", "SONIC_BOOM"},
            Sound.ENTITY_WITHER_AMBIENT,
            Sound.ENTITY_WITHER_DEATH,
            0.5f, 1.0f
    ),
    PLAYER(
            new String[]{"END_ROD", "FLASH"},
            new String[]{"TOTEM", "FLASH", "FIREWORK"},
            Sound.BLOCK_BEACON_POWER_SELECT,
            Sound.UI_TOAST_CHALLENGE_COMPLETE,
            0.9f, 1.0f
    ),
    ANIMAL(
            new String[]{"ASH", "WHITE_SMOKE"},
            new String[]{"CLOUD", "CRIT"},
            Sound.ENTITY_CHICKEN_STEP,
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            0.8f, 1.2f
    );

    private final String[] ambientParticles;
    private final String[] finaleParticles;
    private final Sound ambientSound;
    private final Sound finaleSound;
    private final float ambientPitch;
    private final float finalePitch;

    DeathProfile(String[] ambientParticles, String[] finaleParticles, Sound ambientSound, Sound finaleSound, float ambientPitch, float finalePitch) {
        this.ambientParticles = ambientParticles;
        this.finaleParticles = finaleParticles;
        this.ambientSound = ambientSound;
        this.finaleSound = finaleSound;
        this.ambientPitch = ambientPitch;
        this.finalePitch = finalePitch;
    }

    public String[] getAmbientParticles() {
        return ambientParticles;
    }

    public String[] getFinaleParticles() {
        return finaleParticles;
    }

    public Sound getAmbientSound() {
        return ambientSound;
    }

    public Sound getFinaleSound() {
        return finaleSound;
    }

    public float getAmbientPitch() {
        return ambientPitch;
    }

    public float getFinalePitch() {
        return finalePitch;
    }

    public static DeathProfile fromEntity(Entity entity) {
        if (entity == null) return ANIMAL;
        if (entity instanceof Player) return PLAYER;

        String typeName = entity.getType().name().toUpperCase(Locale.ROOT);

        if (typeName.contains("WITHER") || typeName.contains("DRAGON") || typeName.contains("WARDEN") || typeName.contains("ELDER_GUARDIAN")) {
            return BOSS;
        }

        if (typeName.contains("CREEPER")) {
            return CREEPER;
        }

        if (typeName.contains("ENDERMAN") || typeName.contains("ENDERMITE") || typeName.contains("SHULKER")) {
            return ENDER;
        }

        if (typeName.contains("BLAZE") || typeName.contains("MAGMA") || typeName.contains("GHAST") || typeName.contains("STRIDER")) {
            return INFERNAL;
        }

        if (typeName.contains("SKELETON") || typeName.contains("STRAY")) {
            return SKELETAL;
        }

        if (typeName.contains("ZOMBIE") || typeName.contains("DROWNED") || typeName.contains("HUSK")
                || typeName.contains("PIGLIN") || typeName.contains("HOGLIN") || typeName.contains("PHANTOM")) {
            return UNDEAD;
        }

        if (typeName.contains("SPIDER") || typeName.contains("SILVERFISH")) {
            return ARACHNID;
        }

        if (typeName.contains("GUARDIAN") || typeName.contains("DOLPHIN") || typeName.contains("SQUID")
                || typeName.contains("FISH") || typeName.contains("AXOLOTL") || typeName.contains("TURTLE")) {
            return AQUATIC;
        }

        if (typeName.contains("VILLAGER") || typeName.contains("PILLAGER") || typeName.contains("EVOKER")
                || typeName.contains("VINDICATOR") || typeName.contains("RAVAGER") || typeName.contains("WITCH")
                || typeName.contains("GOLEM") || typeName.contains("SNOWMAN")) {
            return VILLAGER_ILLAGER;
        }

        if (entity instanceof Animals) {
            return ANIMAL;
        }

        if (entity instanceof Monster) {
            return UNDEAD;
        }

        return ANIMAL;
    }

    public static DeathProfile fromName(String name) {
        if (name == null) return null;
        try {
            return DeathProfile.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
