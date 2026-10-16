package me.lhawk.mobvitals.command;

import me.lhawk.mobvitals.config.PluginConfig;
import me.lhawk.mobvitals.death.CorpseManager;
import me.lhawk.mobvitals.death.DeathProfile;
import me.lhawk.mobvitals.indicator.IndicatorManager;
import me.lhawk.mobvitals.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MobVitalsCommand implements CommandExecutor, TabCompleter {
    private final PluginConfig config;
    private final IndicatorManager indicatorManager;
    private final CorpseManager corpseManager;

    public MobVitalsCommand(PluginConfig config, IndicatorManager indicatorManager, CorpseManager corpseManager) {
        this.config = config;
        this.indicatorManager = indicatorManager;
        this.corpseManager = corpseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("mobvitals.admin")) {
                    TextUtil.sendMessage(sender, config.getMsgNoPermission());
                    return true;
                }
                config.load();
                indicatorManager.start();
                corpseManager.start();
                TextUtil.sendMessage(sender, config.getMsgReloaded());
                return true;
            }
            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    TextUtil.sendMessage(sender, config.getMsgPlayerOnly());
                    return true;
                }
                if (!player.hasPermission("mobvitals.use")) {
                    TextUtil.sendMessage(player, config.getMsgNoPermission());
                    return true;
                }
                boolean enabled = indicatorManager.togglePlayer(player);
                if (enabled) {
                    TextUtil.sendMessage(player, config.getMsgToggledOn());
                } else {
                    TextUtil.sendMessage(player, config.getMsgToggledOff());
                }
                return true;
            }
            case "info", "status" -> {
                if (!sender.hasPermission("mobvitals.use")) {
                    TextUtil.sendMessage(sender, config.getMsgNoPermission());
                    return true;
                }
                String info = config.getMsgStatusInfo()
                        .replace("<active_indicators>", String.valueOf(indicatorManager.getActiveIndicatorCount()))
                        .replace("<active_corpses>", String.valueOf(corpseManager.getActiveCorpseCount()))
                        .replace("<active_popups>", String.valueOf(indicatorManager.getActivePopupCount()))
                        .replace("<viewers>", String.valueOf(indicatorManager.getTrackedViewersCount()));
                TextUtil.sendMessage(sender, info);
                return true;
            }
            case "test" -> {
                if (!sender.hasPermission("mobvitals.admin")) {
                    TextUtil.sendMessage(sender, config.getMsgNoPermission());
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    TextUtil.sendMessage(sender, config.getMsgPlayerOnly());
                    return true;
                }
                if (args.length < 2) {
                    String available = Arrays.toString(DeathProfile.values());
                    String msg = config.getMsgUnknownProfile()
                            .replace("<profile>", "none")
                            .replace("<profiles>", available);
                    TextUtil.sendMessage(player, msg);
                    return true;
                }
                String profileName = args[1].toUpperCase(Locale.ROOT);
                DeathProfile profile = DeathProfile.fromName(profileName);
                if (profile == null) {
                    String available = Arrays.toString(DeathProfile.values());
                    String msg = config.getMsgUnknownProfile()
                            .replace("<profile>", args[1])
                            .replace("<profiles>", available);
                    TextUtil.sendMessage(player, msg);
                    return true;
                }
                String started = config.getMsgTestStarted().replace("<profile>", profile.name());
                TextUtil.sendMessage(player, started);
                corpseManager.playPreviewEffect(player, profile);
                return true;
            }
            default -> {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        TextUtil.sendMessage(sender, "<gradient:#FF5555:#FFAA00><bold>MobVitals Commands</bold></gradient> <gray>(v1.0.0)</gray>");
        TextUtil.sendMessage(sender, "<yellow>/" + label + " toggle</yellow> <gray>- Toggle health indicators for yourself</gray>");
        TextUtil.sendMessage(sender, "<yellow>/" + label + " info</yellow> <gray>- Show plugin stats and active visual counts</gray>");
        if (sender.hasPermission("mobvitals.admin")) {
            TextUtil.sendMessage(sender, "<yellow>/" + label + " test <profile></yellow> <gray>- Preview a cinematic death effect</gray>");
            TextUtil.sendMessage(sender, "<yellow>/" + label + " reload</yellow> <gray>- Reload configuration and death profiles</gray>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("toggle");
            list.add("info");
            if (sender.hasPermission("mobvitals.admin")) {
                list.add("reload");
                list.add("test");
            }
            return filterPrefix(list, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("test") && sender.hasPermission("mobvitals.admin")) {
            List<String> profiles = new ArrayList<>();
            for (DeathProfile p : DeathProfile.values()) {
                profiles.add(p.name().toLowerCase(Locale.ROOT));
            }
            return filterPrefix(profiles, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> res = new ArrayList<>();
        for (String c : candidates) {
            if (c.toLowerCase(Locale.ROOT).startsWith(lower)) {
                res.add(c);
            }
        }
        return res;
    }
}
