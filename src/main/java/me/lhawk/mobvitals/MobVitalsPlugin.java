package me.lhawk.mobvitals;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.lhawk.mobvitals.command.MobVitalsCommand;
import me.lhawk.mobvitals.config.PluginConfig;
import me.lhawk.mobvitals.death.CorpseManager;
import me.lhawk.mobvitals.indicator.IndicatorManager;
import me.lhawk.mobvitals.listener.EntityVitalListener;
import me.lhawk.mobvitals.listener.VitalPacketListener;
import me.lhawk.mobvitals.util.Platform;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobVitalsPlugin extends JavaPlugin {
    private PluginConfig config;
    private IndicatorManager indicatorManager;
    private CorpseManager corpseManager;
    private VitalPacketListener packetListener;

    @Override
    public void onEnable() {
        getLogger().info("Initializing MobVitals " + getDescription().getVersion() + " by " + getDescription().getAuthors() + "...");

        if (Platform.isFolia()) {
            getLogger().info("Folia environment detected: Threaded region schedulers enabled.");
        }
        if (Platform.hasPacketEvents()) {
            getLogger().info("PacketEvents detected: Packet-based indicators and corpse visuals activated.");
        } else {
            getLogger().warning("PacketEvents not found! MobVitals requires PacketEvents for packet manipulation.");
        }

        config = new PluginConfig(this);
        config.load();

        indicatorManager = new IndicatorManager(this, config);
        corpseManager = new CorpseManager(this, config);

        if (Platform.hasPacketEvents()) {
            packetListener = new VitalPacketListener(corpseManager);
            PacketEvents.getAPI().getEventManager().registerListener(packetListener);
        }

        indicatorManager.start();
        corpseManager.start();

        getServer().getPluginManager().registerEvents(new EntityVitalListener(this, indicatorManager, corpseManager), this);

        MobVitalsCommand cmdExecutor = new MobVitalsCommand(config, indicatorManager, corpseManager, getDescription().getVersion());
        PluginCommand cmd = getCommand("mobvitals");
        if (cmd != null) {
            cmd.setExecutor(cmdExecutor);
            cmd.setTabCompleter(cmdExecutor);
        }

        getLogger().info("MobVitals enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (indicatorManager != null) {
            indicatorManager.stop();
        }
        if (corpseManager != null) {
            corpseManager.stop();
        }
        if (packetListener != null && Platform.hasPacketEvents()) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            } catch (Throwable ignored) {}
        }
        getLogger().info("MobVitals disabled cleanly.");
    }

    public PluginConfig getPluginConfig() {
        return config;
    }

    public IndicatorManager getIndicatorManager() {
        return indicatorManager;
    }

    public CorpseManager getCorpseManager() {
        return corpseManager;
    }
}
