package me.lhawk.mobvitals.indicator;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.lhawk.mobvitals.config.PluginConfig;
import me.lhawk.mobvitals.util.Platform;
import me.lhawk.mobvitals.util.Schedulers;
import me.lhawk.mobvitals.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class IndicatorManager {
    private final Plugin plugin;
    private final PluginConfig config;

    private final AtomicInteger indicatorIdCounter = new AtomicInteger(-100000);
    private final Map<Integer, HealthIndicator> activeIndicators = new ConcurrentHashMap<>();
    private final Set<UUID> toggledOffPlayers = ConcurrentHashMap.newKeySet();

    private Schedulers.TaskHandle updateTask;

    public IndicatorManager(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();
        long interval = config.getUpdateIntervalTicks();
        updateTask = Schedulers.timer(plugin, 4L, interval, this::tick);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        if (Platform.hasPacketEvents()) {
            for (HealthIndicator indicator : activeIndicators.values()) {
                destroyForViewers(indicator);
            }
        }
        activeIndicators.clear();
    }

    public boolean togglePlayer(Player player) {
        if (player == null) return false;
        UUID uid = player.getUniqueId();
        if (toggledOffPlayers.contains(uid)) {
            toggledOffPlayers.remove(uid);
            return true;
        } else {
            toggledOffPlayers.add(uid);
            clearIndicatorsForPlayer(player);
            return false;
        }
    }

    public boolean isToggledOff(UUID uuid) {
        return toggledOffPlayers.contains(uuid);
    }

    public void onPlayerQuit(Player player) {
        if (player == null) return;
        UUID uid = player.getUniqueId();
        toggledOffPlayers.remove(uid);
        for (HealthIndicator indicator : activeIndicators.values()) {
            indicator.removeViewer(uid);
        }
    }

    public void onEntityDamageOrHeal(LivingEntity entity) {
        if (!config.isIndicatorsEnabled() || entity == null || !isEligible(entity)) return;

        int entityId = entity.getEntityId();
        HealthIndicator indicator = activeIndicators.get(entityId);
        double health = entity.getHealth();
        double maxHealth = getMaxHealth(entity);

        if (indicator == null) {
            indicator = new HealthIndicator(entity, getNextIndicatorId(), entity.getLocation(), health, maxHealth);
            activeIndicators.put(entityId, indicator);
        } else {
            indicator.setLastHealth(health);
            indicator.setLastMaxHealth(maxHealth);
            indicator.markDamaged();
        }

        Component comp = formatIndicator(entity, health, maxHealth);
        WrapperPlayServerEntityMetadata metaPacket = createMetadataPacket(indicator.getVirtualIndicatorId(), comp);

        if (Platform.hasPacketEvents()) {
            for (UUID uid : indicator.getViewers()) {
                Player p = plugin.getServer().getPlayer(uid);
                if (p != null && p.isOnline()) {
                    try {
                        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(p, metaPacket);
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    public void removeEntity(int entityId) {
        HealthIndicator indicator = activeIndicators.remove(entityId);
        if (indicator == null || !Platform.hasPacketEvents()) return;
        destroyForViewers(indicator);
    }

    private void tick() {
        if (!config.isIndicatorsEnabled() || !Platform.hasPacketEvents()) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (toggledOffPlayers.contains(player.getUniqueId())) continue;
            Schedulers.runForEntity(plugin, player, () -> scanPlayerView(player));
        }

        activeIndicators.entrySet().removeIf(entry -> {
            HealthIndicator ind = entry.getValue();
            LivingEntity host = ind.getHostEntity();
            if (host == null || !host.isValid() || host.isDead() || host.getHealth() <= 0) {
                destroyForViewers(ind);
                return true;
            }

            Location currentLoc = host.getLocation();
            Location lastLoc = ind.getLastLocation();
            if (lastLoc == null || lastLoc.distanceSquared(currentLoc) > 0.002) {
                teleportIndicatorForViewers(ind, host, currentLoc);
                ind.setLastLocation(currentLoc.clone());
            }

            double currentHealth = host.getHealth();
            if (Math.abs(currentHealth - ind.getLastHealth()) > 0.05) {
                ind.setLastHealth(currentHealth);
                updateMetadataForViewers(ind, host);
            }

            return false;
        });
    }

    private void scanPlayerView(Player player) {
        if (!player.isOnline() || toggledOffPlayers.contains(player.getUniqueId())) return;

        Location pLoc = player.getLocation();
        if (pLoc.getWorld() == null) return;

        double viewDist = config.getViewDistance();
        double viewDistSq = viewDist * viewDist;
        UUID pUid = player.getUniqueId();

        Set<Integer> visibleInThisScan = new HashSet<>();

        for (Entity ent : player.getNearbyEntities(viewDist, viewDist, viewDist)) {
            if (!(ent instanceof LivingEntity living) || !isEligible(living)) continue;
            if (ent.getEntityId() == player.getEntityId()) continue;

            Location eLoc = ent.getLocation();
            if (eLoc.distanceSquared(pLoc) > viewDistSq) continue;

            int entityId = ent.getEntityId();
            visibleInThisScan.add(entityId);

            double health = living.getHealth();
            double maxHealth = getMaxHealth(living);

            HealthIndicator indicator = activeIndicators.computeIfAbsent(entityId, k ->
                    new HealthIndicator(living, getNextIndicatorId(), eLoc.clone(), health, maxHealth)
            );

            boolean isNewViewer = indicator.addViewer(pUid);
            if (isNewViewer) {
                spawnIndicatorForPlayer(player, indicator, living, eLoc, health, maxHealth);
            }
        }

        for (Map.Entry<Integer, HealthIndicator> entry : activeIndicators.entrySet()) {
            int entityId = entry.getKey();
            HealthIndicator ind = entry.getValue();
            if (!visibleInThisScan.contains(entityId) && ind.removeViewer(pUid)) {
                destroyIndicatorForPlayer(player, ind);
            }
        }
    }

    private void spawnIndicatorForPlayer(Player player, HealthIndicator indicator, LivingEntity entity, Location loc, double health, double maxHealth) {
        if (!Platform.hasPacketEvents()) return;
        try {
            double yOffset = entity.getHeight() + config.getHeightOffset() - 0.95;
            Vector3d pos = new Vector3d(loc.getX(), loc.getY() + yOffset, loc.getZ());
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                    indicator.getVirtualIndicatorId(),
                    Optional.of(UUID.randomUUID()),
                    EntityTypes.ARMOR_STAND,
                    pos,
                    0f, 0f, 0f, 0,
                    Optional.empty()
            );

            Component comp = formatIndicator(entity, health, maxHealth);
            WrapperPlayServerEntityMetadata meta = createMetadataPacket(indicator.getVirtualIndicatorId(), comp);

            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, spawn);
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, meta);
        } catch (Throwable ignored) {}
    }

    private void teleportIndicatorForViewers(HealthIndicator indicator, LivingEntity entity, Location loc) {
        if (!Platform.hasPacketEvents()) return;
        try {
            double yOffset = entity.getHeight() + config.getHeightOffset() - 0.95;
            Vector3d pos = new Vector3d(loc.getX(), loc.getY() + yOffset, loc.getZ());
            WrapperPlayServerEntityTeleport tp = new WrapperPlayServerEntityTeleport(
                    indicator.getVirtualIndicatorId(),
                    pos,
                    0f, 0f, false
            );
            for (UUID uid : indicator.getViewers()) {
                Player p = plugin.getServer().getPlayer(uid);
                if (p != null && p.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacketSilently(p, tp);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void updateMetadataForViewers(HealthIndicator indicator, LivingEntity entity) {
        if (!Platform.hasPacketEvents()) return;
        try {
            Component comp = formatIndicator(entity, entity.getHealth(), getMaxHealth(entity));
            WrapperPlayServerEntityMetadata meta = createMetadataPacket(indicator.getVirtualIndicatorId(), comp);
            for (UUID uid : indicator.getViewers()) {
                Player p = plugin.getServer().getPlayer(uid);
                if (p != null && p.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacketSilently(p, meta);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void destroyIndicatorForPlayer(Player player, HealthIndicator indicator) {
        if (!Platform.hasPacketEvents()) return;
        try {
            WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(indicator.getVirtualIndicatorId());
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, destroy);
        } catch (Throwable ignored) {}
    }

    private void destroyForViewers(HealthIndicator indicator) {
        if (!Platform.hasPacketEvents() || indicator == null) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(indicator.getVirtualIndicatorId());
        for (UUID uid : indicator.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                try {
                    PacketEvents.getAPI().getPlayerManager().sendPacketSilently(p, destroy);
                } catch (Throwable ignored) {}
            }
        }
        indicator.getViewers().clear();
    }

    private void clearIndicatorsForPlayer(Player player) {
        if (player == null || !Platform.hasPacketEvents()) return;
        UUID pUid = player.getUniqueId();
        for (HealthIndicator ind : activeIndicators.values()) {
            if (ind.removeViewer(pUid)) {
                destroyIndicatorForPlayer(player, ind);
            }
        }
    }

    private WrapperPlayServerEntityMetadata createMetadataPacket(int virtualId, Component component) {
        List<EntityData<?>> metadata = new ArrayList<>(5);
        metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20));
        metadata.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(component)));
        metadata.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));
        metadata.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));

        int flagsIndex = 15;
        try {
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) {
                flagsIndex = 14;
            }
        } catch (Throwable ignored) {}

        metadata.add(new EntityData<>(flagsIndex, EntityDataTypes.BYTE, (byte) (0x01 | 0x08 | 0x10)));
        return new WrapperPlayServerEntityMetadata(virtualId, metadata);
    }

    public Component formatIndicator(LivingEntity entity, double health, double maxHealth) {
        double clampedHealth = Math.max(0.0, health);
        double clampedMax = Math.max(0.1, maxHealth);
        double fraction = Math.max(0.0, Math.min(1.0, clampedHealth / clampedMax));

        int barLength = config.getBarLength();
        int filled = (int) Math.round(fraction * barLength);
        int empty = Math.max(0, barLength - filled);

        String color;
        if (fraction >= 0.75) {
            color = config.getColorHigh();
        } else if (fraction >= 0.50) {
            color = config.getColorMedium();
        } else if (fraction >= 0.25) {
            color = config.getColorLow();
        } else {
            color = config.getColorCritical();
        }

        StringBuilder bar = new StringBuilder();
        bar.append(color);
        for (int i = 0; i < filled; i++) {
            bar.append(config.getSymbolFull());
        }
        bar.append(config.getColorEmpty());
        for (int i = 0; i < empty; i++) {
            bar.append(config.getSymbolEmpty());
        }
        bar.append("<reset>");

        String name;
        if (entity instanceof Player) {
            name = entity.getName();
        } else if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            name = entity.getCustomName();
        } else {
            name = formatEntityTypeName(entity.getType().name());
        }

        String currentStr = (clampedHealth == (long) clampedHealth) ? String.valueOf((long) clampedHealth) : String.format(Locale.ROOT, "%.1f", clampedHealth);
        String maxStr = (clampedMax == (long) clampedMax) ? String.valueOf((long) clampedMax) : String.format(Locale.ROOT, "%.1f", clampedMax);
        String percentStr = String.valueOf((int) Math.round(fraction * 100));

        String template;
        if (entity instanceof Player) {
            template = config.getPlayerFormat();
        } else if (isBoss(entity)) {
            template = config.getBossFormat();
        } else {
            template = config.getFormat();
        }

        String formatted = template
                .replace("<name>", name)
                .replace("<bar>", bar.toString())
                .replace("<color>", color)
                .replace("<current>", currentStr)
                .replace("<max>", maxStr)
                .replace("<percent>", percentStr);

        return TextUtil.parse(formatted);
    }

    private boolean isEligible(LivingEntity entity) {
        if (entity == null || !entity.isValid() || entity.isDead()) return false;
        if (entity.getHealth() <= 0) return false;

        String typeName = entity.getType().name().toUpperCase(Locale.ROOT);
        if (config.getBlacklist().contains(typeName)) return false;

        if (entity instanceof Player) {
            return config.isShowForPlayers();
        }

        if (isBoss(entity)) {
            return config.isShowForBosses();
        }

        if (entity instanceof Villager) {
            return config.isShowForVillagers();
        }

        if (entity instanceof Monster) {
            return config.isShowForMonsters();
        }

        if (entity instanceof Animals) {
            return config.isShowForAnimals();
        }

        return true;
    }

    private boolean isBoss(LivingEntity entity) {
        String tn = entity.getType().name().toUpperCase(Locale.ROOT);
        return tn.contains("WITHER") || tn.contains("DRAGON") || tn.contains("WARDEN") || tn.contains("ELDER_GUARDIAN");
    }

    private double getMaxHealth(LivingEntity entity) {
        try {
            AttributeInstance attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) return attr.getValue();
        } catch (Throwable ignored) {}
        try {
            Method m = entity.getClass().getMethod("getMaxHealth");
            return (double) m.invoke(entity);
        } catch (Throwable ignored) {
            return 20.0;
        }
    }

    private String formatEntityTypeName(String rawName) {
        if (rawName == null) return "Entity";
        String[] parts = rawName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
                if (i < parts.length - 1) sb.append(" ");
            }
        }
        return sb.toString();
    }

    public int getNextIndicatorId() {
        return indicatorIdCounter.decrementAndGet();
    }

    public int getActiveIndicatorCount() {
        return activeIndicators.size();
    }

    public int getTrackedViewersCount() {
        Set<UUID> all = new HashSet<>();
        for (HealthIndicator ind : activeIndicators.values()) {
            all.addAll(ind.getViewers());
        }
        return all.size();
    }
}
