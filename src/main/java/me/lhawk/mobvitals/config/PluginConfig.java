package me.lhawk.mobvitals.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PluginConfig {
    private final Plugin plugin;

    private boolean generalEnabled;
    private double viewDistance;
    private long updateIntervalTicks;

    private boolean indicatorsEnabled;
    private double heightOffset;
    private boolean showForPlayers;
    private boolean showForMonsters;
    private boolean showForAnimals;
    private boolean showForVillagers;
    private boolean showForBosses;
    private boolean showOnlyWhenDamaged;
    private double damagedDisplaySeconds;
    private int barLength;
    private String symbolFull;
    private String symbolEmpty;
    private String colorHigh;
    private String colorMedium;
    private String colorLow;
    private String colorCritical;
    private String colorEmpty;
    private String format;
    private String playerFormat;
    private String bossFormat;
    private Set<String> blacklist = new HashSet<>();

    private boolean popupsEnabled;
    private double popupDurationSeconds;
    private String damageFormat;
    private String healFormat;
    private double popupThreshold;

    private boolean deathAnimationEnabled;
    private double corpseDurationSeconds;
    private boolean enableCorpseBody;
    private boolean enableAmbientParticles;
    private long ambientParticleRateTicks;
    private boolean denseCorpseReduction;
    private double denseRadius;
    private boolean enableFinalExplosion;
    private boolean enableSoundEffects;
    private boolean enableForPlayers;

    private String msgPrefix;
    private String msgReloaded;
    private String msgToggledOn;
    private String msgToggledOff;
    private String msgStatusInfo;
    private String msgTestStarted;
    private String msgUnknownProfile;
    private String msgNoPermission;
    private String msgPlayerOnly;

    public PluginConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        generalEnabled = config.getBoolean("general.enabled", true);
        viewDistance = Math.max(8.0, config.getDouble("general.view-distance", 32.0));
        updateIntervalTicks = Math.max(1, config.getLong("general.update-interval-ticks", 2));

        indicatorsEnabled = config.getBoolean("indicators.enabled", true);
        heightOffset = config.getDouble("indicators.height-offset", 0.35);
        showForPlayers = config.getBoolean("indicators.show-for-players", true);
        showForMonsters = config.getBoolean("indicators.show-for-monsters", true);
        showForAnimals = config.getBoolean("indicators.show-for-animals", true);
        showForVillagers = config.getBoolean("indicators.show-for-villagers", true);
        showForBosses = config.getBoolean("indicators.show-for-bosses", true);
        showOnlyWhenDamaged = config.getBoolean("indicators.show-only-when-damaged", false);
        damagedDisplaySeconds = Math.max(0.5, config.getDouble("indicators.damaged-display-seconds", 5.0));
        barLength = Math.max(3, Math.min(30, config.getInt("indicators.bar-length", 10)));
        symbolFull = config.getString("indicators.symbol-full", "■");
        symbolEmpty = config.getString("indicators.symbol-empty", "□");
        colorHigh = config.getString("indicators.color-high", "<green>");
        colorMedium = config.getString("indicators.color-medium", "<yellow>");
        colorLow = config.getString("indicators.color-low", "<gold>");
        colorCritical = config.getString("indicators.color-critical", "<red>");
        colorEmpty = config.getString("indicators.color-empty", "<dark_gray>");
        format = config.getString("indicators.format", "<name> <bar> <color><current></color><gray>/</gray><green><max></green> <red>❤</red>");
        playerFormat = config.getString("indicators.player-format", "<gold><name></gold> <bar> <color><current></color><gray>/</gray><green><max></green> <red>❤</red>");
        bossFormat = config.getString("indicators.boss-format", "<dark_purple><bold><name></bold></dark_purple> <bar> <color><current></color><gray>/</gray><green><max></green> <red>❤</red> <yellow>(<percent>%)</yellow>");

        List<String> rawBlacklist = config.getStringList("indicators.blacklist");
        blacklist = new HashSet<>();
        for (String b : rawBlacklist) {
            if (b != null && !b.trim().isEmpty()) {
                blacklist.add(b.trim().toUpperCase(Locale.ROOT));
            }
        }

        popupsEnabled = config.getBoolean("variation-popups.enabled", true);
        popupDurationSeconds = Math.max(0.5, Math.min(10.0, config.getDouble("variation-popups.duration-seconds", 1.8)));
        damageFormat = config.getString("variation-popups.damage-format", "<red>-<bold><value></bold> <dark_red>❤</dark_red>");
        healFormat = config.getString("variation-popups.heal-format", "<green>+<bold><value></bold> <emerald>💚</emerald>");
        popupThreshold = Math.max(0.01, config.getDouble("variation-popups.threshold", 0.1));

        deathAnimationEnabled = config.getBoolean("death-animation.enabled", true);
        corpseDurationSeconds = Math.max(1.0, Math.min(30.0, config.getDouble("death-animation.corpse-duration-seconds", 4.0)));
        enableCorpseBody = config.getBoolean("death-animation.enable-corpse-body", true);
        enableAmbientParticles = config.getBoolean("death-animation.enable-ambient-particles", true);
        ambientParticleRateTicks = Math.max(1, config.getLong("death-animation.ambient-particle-rate-ticks", 4));
        denseCorpseReduction = config.getBoolean("death-animation.dense-corpse-reduction", true);
        denseRadius = Math.max(1.0, config.getDouble("death-animation.dense-radius", 4.0));
        enableFinalExplosion = config.getBoolean("death-animation.enable-final-explosion", true);
        enableSoundEffects = config.getBoolean("death-animation.enable-sound-effects", true);
        enableForPlayers = config.getBoolean("death-animation.enable-for-players", true);

        msgPrefix = config.getString("messages.prefix", "<dark_gray>[<gradient:#FF5555:#FFAA00>MobVitals</gradient>]<reset> ");
        msgReloaded = config.getString("messages.reloaded", "<green>Configuration reloaded successfully.</green>");
        msgToggledOn = config.getString("messages.toggled-on", "<green>Health indicators are now <bold>ENABLED</bold> for your view.</green>");
        msgToggledOff = config.getString("messages.toggled-off", "<red>Health indicators are now <bold>DISABLED</bold> for your view.</red>");
        msgStatusInfo = config.getString("messages.status-info", "<yellow>MobVitals Status:<newline> <gray>• Active Indicators:</gray> <white><active_indicators></white><newline> <gray>• Active Corpses:</gray> <white><active_corpses></white><newline> <gray>• Tracked Viewers:</gray> <white><viewers></white>");
        msgTestStarted = config.getString("messages.test-started", "<yellow>Executing death animation preview for profile: <gold><profile></gold> at your location.</yellow>");
        msgUnknownProfile = config.getString("messages.unknown-profile", "<red>Unknown profile '<gold><profile></gold>'. Available profiles: <white><profiles></white></red>");
        msgNoPermission = config.getString("messages.no-permission", "<red>You do not have permission to execute this command.</red>");
        msgPlayerOnly = config.getString("messages.player-only", "<red>This command can only be executed by in-game players.</red>");
    }

    public boolean isGeneralEnabled() {
        return generalEnabled;
    }

    public double getViewDistance() {
        return viewDistance;
    }

    public long getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }

    public boolean isIndicatorsEnabled() {
        return generalEnabled && indicatorsEnabled;
    }

    public double getHeightOffset() {
        return heightOffset;
    }

    public boolean isShowForPlayers() {
        return showForPlayers;
    }

    public boolean isShowForMonsters() {
        return showForMonsters;
    }

    public boolean isShowForAnimals() {
        return showForAnimals;
    }

    public boolean isShowForVillagers() {
        return showForVillagers;
    }

    public boolean isShowForBosses() {
        return showForBosses;
    }

    public boolean isShowOnlyWhenDamaged() {
        return showOnlyWhenDamaged;
    }

    public double getDamagedDisplaySeconds() {
        return damagedDisplaySeconds;
    }

    public int getBarLength() {
        return barLength;
    }

    public String getSymbolFull() {
        return symbolFull;
    }

    public String getSymbolEmpty() {
        return symbolEmpty;
    }

    public String getColorHigh() {
        return colorHigh;
    }

    public String getColorMedium() {
        return colorMedium;
    }

    public String getColorLow() {
        return colorLow;
    }

    public String getColorCritical() {
        return colorCritical;
    }

    public String getColorEmpty() {
        return colorEmpty;
    }

    public String getFormat() {
        return format;
    }

    public String getPlayerFormat() {
        return playerFormat;
    }

    public String getBossFormat() {
        return bossFormat;
    }

    public Set<String> getBlacklist() {
        return Collections.unmodifiableSet(blacklist);
    }

    public boolean isDeathAnimationEnabled() {
        return generalEnabled && deathAnimationEnabled;
    }

    public double getCorpseDurationSeconds() {
        return corpseDurationSeconds;
    }

    public boolean isEnableCorpseBody() {
        return enableCorpseBody;
    }

    public boolean isEnableAmbientParticles() {
        return enableAmbientParticles;
    }

    public long getAmbientParticleRateTicks() {
        return ambientParticleRateTicks;
    }

    public boolean isEnableFinalExplosion() {
        return enableFinalExplosion;
    }

    public boolean isEnableSoundEffects() {
        return enableSoundEffects;
    }

    public boolean isEnableForPlayers() {
        return enableForPlayers;
    }

    public boolean isPopupsEnabled() {
        return popupsEnabled;
    }

    public double getPopupDurationSeconds() {
        return popupDurationSeconds;
    }

    public String getDamageFormat() {
        return damageFormat;
    }

    public String getHealFormat() {
        return healFormat;
    }

    public double getPopupThreshold() {
        return popupThreshold;
    }

    public boolean isDenseCorpseReduction() {
        return denseCorpseReduction;
    }

    public double getDenseRadius() {
        return denseRadius;
    }

    public String getMsgPrefix() {
        return msgPrefix;
    }

    public String getMsgReloaded() {
        return msgPrefix + msgReloaded;
    }

    public String getMsgToggledOn() {
        return msgPrefix + msgToggledOn;
    }

    public String getMsgToggledOff() {
        return msgPrefix + msgToggledOff;
    }

    public String getMsgStatusInfo() {
        return msgPrefix + msgStatusInfo;
    }

    public String getMsgTestStarted() {
        return msgPrefix + msgTestStarted;
    }

    public String getMsgUnknownProfile() {
        return msgPrefix + msgUnknownProfile;
    }

    public String getMsgNoPermission() {
        return msgPrefix + msgNoPermission;
    }

    public String getMsgPlayerOnly() {
        return msgPrefix + msgPlayerOnly;
    }
}
