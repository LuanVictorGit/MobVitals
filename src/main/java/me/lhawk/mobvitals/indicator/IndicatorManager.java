package me.lhawk.mobvitals.indicator;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class IndicatorManager {
    private final Plugin plugin;
    private final PluginConfig config;

    private final AtomicInteger indicatorIdCounter = new AtomicInteger(1_500_000_000);
    private final Map<Integer, HealthIndicator> activeIndicators = new ConcurrentHashMap<>();
    private final Map<Integer, VariationPopup> activePopups = new ConcurrentHashMap<>();
    private final Map<Integer, Double> recordedHealthMap = new ConcurrentHashMap<>();
    private final Set<UUID> toggledOffPlayers = ConcurrentHashMap.newKeySet();

    private Schedulers.TaskHandle updateTask;

    public IndicatorManager(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();
        long interval = config.getUpdateIntervalTicks();
        updateTask = Schedulers.timer(plugin, 2L, interval, this::tick);
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
            for (VariationPopup popup : activePopups.values()) {
                destroyPopupForViewers(popup);
            }
        }
        activeIndicators.clear();
        activePopups.clear();
        recordedHealthMap.clear();
        toggledOffPlayers.clear();
    }

    public boolean togglePlayer(Player player) {
        if (player == null) return false;
        UUID uid = player.getUniqueId();
        if (toggledOffPlayers.contains(uid)) {
            toggledOffPlayers.remove(uid);
            return true;
        } else {
            toggledOffPlayers.add(uid);
            clearVisualsForPlayer(player);
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
        for (VariationPopup popup : activePopups.values()) {
            popup.removeViewer(uid);
        }
    }

    public double getEffectiveHealth(LivingEntity entity) {
        if (entity == null || entity.isDead()) return 0.0;
        double health = Math.max(0.0, entity.getHealth());
        double absorption = 0.0;
        try {
            absorption = entity.getAbsorptionAmount();
        } catch (Throwable ignored) {}
        return health + Math.max(0.0, absorption);
    }

    public void recordEntityHealth(LivingEntity entity) {
        if (entity == null || !entity.isValid() || entity.isDead()) return;
        recordedHealthMap.putIfAbsent(entity.getEntityId(), getEffectiveHealth(entity));
    }

    public void onDamage(LivingEntity entity, double finalDamage) {
        if (!config.isIndicatorsEnabled() || entity == null || !isEligibleType(entity)) return;

        int entityId = entity.getEntityId();
        double currentHealth = Math.max(0.0, entity.getHealth());
        double damageAmount = Math.max(0.0, Math.min(currentHealth, finalDamage));
        double healthAfter = Math.max(0.0, currentHealth - damageAmount);
        double maxHealth = getMaxHealth(entity);

        recordedHealthMap.put(entityId, healthAfter);

        if (damageAmount >= config.getPopupThreshold() && config.isPopupsEnabled()) {
            spawnVariationPopup(entity, damageAmount, true);
        }

        if (healthAfter > 0.0) {
            showOrUpdateIndicator(entity, healthAfter, maxHealth);
        }
    }

    public void onHeal(LivingEntity entity, double amount) {
        if (!config.isIndicatorsEnabled() || entity == null || !isEligibleType(entity)) return;

        int entityId = entity.getEntityId();
        double currentHealth = Math.max(0.0, entity.getHealth());
        double maxHealth = getMaxHealth(entity);
        double healthAfter = Math.min(maxHealth, currentHealth + amount);
        double actualHeal = Math.max(0.0, healthAfter - currentHealth);

        recordedHealthMap.put(entityId, healthAfter);

        if (actualHeal >= config.getPopupThreshold() && config.isPopupsEnabled()) {
            spawnVariationPopup(entity, actualHeal, false);
        }

        showOrUpdateIndicator(entity, healthAfter, maxHealth);
    }

    public void handleHealthChange(LivingEntity entity) {
        if (!config.isIndicatorsEnabled() || entity == null || !isEligible(entity)) return;

        int entityId = entity.getEntityId();
        double currentHealth = getEffectiveHealth(entity);
        double maxHealth = getMaxHealth(entity);

        Double previousHealthObj = recordedHealthMap.put(entityId, currentHealth);
        double previousHealth = previousHealthObj != null ? previousHealthObj : maxHealth;
        double diff = previousHealth - currentHealth;

        if (Math.abs(diff) >= config.getPopupThreshold() && config.isPopupsEnabled()) {
            if (diff > 0.0) {
                spawnVariationPopup(entity, diff, true);
            } else {
                spawnVariationPopup(entity, -diff, false);
            }
        }

        showOrUpdateIndicator(entity, currentHealth, maxHealth);
    }

    public void showOrUpdateIndicator(LivingEntity entity, double currentHealth, double maxHealth) {
        if (!Platform.hasPacketEvents() || !config.isIndicatorsEnabled() || entity == null) return;

        int entityId = entity.getEntityId();
        HealthIndicator indicator = activeIndicators.get(entityId);
        if (indicator == null) {
            indicator = new HealthIndicator(entity, getNextIndicatorId(), entity.getLocation(), currentHealth, maxHealth);
            activeIndicators.put(entityId, indicator);
            updateIndicatorViewers(indicator, entity);
        } else {
            indicator.setLastHealth(currentHealth);
            indicator.setLastMaxHealth(maxHealth);
            indicator.markDamaged();
            updateMetadataForViewers(indicator, entity);
        }
    }

    public void handleEntityDeath(LivingEntity entity) {
        if (entity == null) return;
        int entityId = entity.getEntityId();
        Double previousHealthObj = recordedHealthMap.remove(entityId);
        double fatalDiff = previousHealthObj != null ? previousHealthObj : getMaxHealth(entity);
        if (fatalDiff >= config.getPopupThreshold() && config.isPopupsEnabled() && config.isIndicatorsEnabled() && isEligibleType(entity)) {
            if (!activePopups.containsKey(entityId)) {
                spawnVariationPopup(entity, fatalDiff, true);
            }
        }
        removeEntity(entityId);
    }

    public void removeEntity(int entityId) {
        recordedHealthMap.remove(entityId);
        HealthIndicator indicator = activeIndicators.remove(entityId);
        if (indicator == null || !Platform.hasPacketEvents()) return;
        destroyForViewers(indicator);
    }

    private void tick() {
        if (!config.isIndicatorsEnabled() || !Platform.hasPacketEvents()) return;

        long now = System.currentTimeMillis();
        long maxDamageAgeMs = (long) (config.getDamagedDisplaySeconds() * 1000.0);

        activeIndicators.entrySet().removeIf(entry -> {
            HealthIndicator ind = entry.getValue();
            LivingEntity host = ind.getHostEntity();
            if (host == null || !host.isValid() || host.isDead() || host.getHealth() <= 0) {
                destroyForViewers(ind);
                return true;
            }

            if (config.isShowOnlyWhenDamaged() && (now - ind.getLastDamageTime() > maxDamageAgeMs)) {
                destroyForViewers(ind);
                return true;
            }

            Location currentLoc = host.getLocation();
            Location lastLoc = ind.getLastLocation();
            if (lastLoc == null || lastLoc.distanceSquared(currentLoc) > 0.002) {
                teleportIndicatorForViewers(ind, host, currentLoc);
                ind.setLastLocation(currentLoc.clone());
            }

            double currentHealth = getEffectiveHealth(host);
            if (Math.abs(currentHealth - ind.getLastHealth()) > 0.05) {
                ind.setLastHealth(currentHealth);
                recordedHealthMap.put(host.getEntityId(), currentHealth);
                updateMetadataForViewers(ind, host);
            }

            updateIndicatorViewers(ind, host);
            return false;
        });

        activePopups.entrySet().removeIf(entry -> {
            VariationPopup popup = entry.getValue();
            if (now >= popup.getExpireTimeMillis()) {
                destroyPopupForViewers(popup);
                return true;
            }

            Location pLoc = popup.getLocation();
            pLoc.add(0.0, 0.035, 0.0);
            teleportPopupForViewers(popup, pLoc);
            return false;
        });
    }

    public void spawnVariationPopup(LivingEntity entity, double amount, boolean isDamage) {
        if (!Platform.hasPacketEvents() || !config.isPopupsEnabled() || entity == null) return;

        Location eLoc = entity.getLocation();
        if (eLoc.getWorld() == null) return;

        double jitterX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.7;
        double jitterZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.7;
        double spawnY = eLoc.getY() + Math.max(0.35, entity.getHeight() * 0.65) + (ThreadLocalRandom.current().nextDouble() * 0.25);

        Location popupLoc = new Location(eLoc.getWorld(), eLoc.getX() + jitterX, spawnY, eLoc.getZ() + jitterZ);
        String valStr = (amount == (long) amount) ? String.valueOf((long) amount) : String.format(Locale.ROOT, "%.1f", amount);
        String template = isDamage ? config.getDamageFormat() : config.getHealFormat();
        String formatted = template.replace("<value>", valStr);
        Component component = TextUtil.parse(formatted);

        int popupId = getNextIndicatorId();
        long expireTime = System.currentTimeMillis() + (long) (config.getPopupDurationSeconds() * 1000.0);
        VariationPopup popup = new VariationPopup(popupId, popupLoc, component, expireTime);

        double viewDistSq = config.getViewDistance() * config.getViewDistance();
        for (Player p : eLoc.getWorld().getPlayers()) {
            if (toggledOffPlayers.contains(p.getUniqueId())) continue;
            if (p.getLocation().distanceSquared(eLoc) <= viewDistSq) {
                if (popup.addViewer(p.getUniqueId())) {
                    spawnPopupForPlayer(p, popup);
                }
            }
        }

        activePopups.put(popupId, popup);
    }

    private void updateIndicatorViewers(HealthIndicator indicator, LivingEntity host) {
        Location loc = host.getLocation();
        if (loc.getWorld() == null) return;

        double viewDistSq = config.getViewDistance() * config.getViewDistance();

        for (Player p : loc.getWorld().getPlayers()) {
            UUID pUid = p.getUniqueId();
            if (toggledOffPlayers.contains(pUid)) continue;

            boolean inRange = p.getLocation().distanceSquared(loc) <= viewDistSq;
            if (inRange) {
                if (indicator.addViewer(pUid)) {
                    spawnIndicatorForPlayer(p, indicator, host, loc, host.getHealth(), getMaxHealth(host));
                }
            } else {
                if (indicator.removeViewer(pUid)) {
                    destroyIndicatorForPlayer(p, indicator.getVirtualIndicatorId());
                }
            }
        }
    }

    private void spawnIndicatorForPlayer(Player player, HealthIndicator indicator, LivingEntity entity, Location loc, double health, double maxHealth) {
        if (!Platform.hasPacketEvents() || player == null || !player.isOnline()) return;

        Component comp = formatIndicator(entity, health, maxHealth);
        int virtualId = indicator.getVirtualIndicatorId();

        double yOffset = entity.getHeight() + config.getHeightOffset() - 1.25;
        Vector3d pos = new Vector3d(loc.getX(), loc.getY() + yOffset, loc.getZ());

        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                virtualId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ARMOR_STAND,
                pos,
                0f, 0f, 0f, 0,
                Optional.empty()
        );

        WrapperPlayServerEntityMetadata meta = createMetadataPacket(virtualId, comp);

        sendPacket(player, spawn);
        sendPacket(player, meta);
    }

    private void spawnPopupForPlayer(Player player, VariationPopup popup) {
        if (!Platform.hasPacketEvents() || player == null || !player.isOnline()) return;

        int virtualId = popup.getVirtualId();
        Location loc = popup.getLocation();
        Vector3d pos = new Vector3d(loc.getX(), loc.getY() - 1.25, loc.getZ());

        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                virtualId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ARMOR_STAND,
                pos,
                0f, 0f, 0f, 0,
                Optional.empty()
        );

        WrapperPlayServerEntityMetadata meta = createMetadataPacket(virtualId, popup.getComponent());

        sendPacket(player, spawn);
        sendPacket(player, meta);
    }

    private void teleportIndicatorForViewers(HealthIndicator indicator, LivingEntity entity, Location loc) {
        if (!Platform.hasPacketEvents()) return;
        double yOffset = entity.getHeight() + config.getHeightOffset() - 1.25;
        Vector3d pos = new Vector3d(loc.getX(), loc.getY() + yOffset, loc.getZ());
        WrapperPlayServerEntityTeleport tp = new WrapperPlayServerEntityTeleport(
                indicator.getVirtualIndicatorId(),
                pos,
                0f, 0f, false
        );

        for (UUID uid : indicator.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendPacket(p, tp);
            }
        }
    }

    private void teleportPopupForViewers(VariationPopup popup, Location loc) {
        if (!Platform.hasPacketEvents()) return;
        Vector3d pos = new Vector3d(loc.getX(), loc.getY() - 1.25, loc.getZ());
        WrapperPlayServerEntityTeleport tp = new WrapperPlayServerEntityTeleport(
                popup.getVirtualId(),
                pos,
                0f, 0f, false
        );

        for (UUID uid : popup.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendPacket(p, tp);
            }
        }
    }

    private void updateMetadataForViewers(HealthIndicator indicator, LivingEntity entity) {
        if (!Platform.hasPacketEvents()) return;
        Component comp = formatIndicator(entity, indicator.getLastHealth(), indicator.getLastMaxHealth());
        WrapperPlayServerEntityMetadata meta = createMetadataPacket(indicator.getVirtualIndicatorId(), comp);

        for (UUID uid : indicator.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendPacket(p, meta);
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

    private void sendPacket(Player player, PacketWrapper<?> packet) {
        if (!Platform.hasPacketEvents() || player == null || !player.isOnline()) return;
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Throwable t) {
            plugin.getLogger().warning("PacketEvents send error: " + t.getMessage());
        }
    }

    private void destroyIndicatorForPlayer(Player player, int virtualId) {
        if (!Platform.hasPacketEvents() || player == null || !player.isOnline()) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(virtualId);
        sendPacket(player, destroy);
    }

    private void destroyForViewers(HealthIndicator indicator) {
        if (!Platform.hasPacketEvents() || indicator == null) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(indicator.getVirtualIndicatorId());
        for (UUID uid : indicator.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendPacket(p, destroy);
            }
        }
        indicator.getViewers().clear();
    }

    private void destroyPopupForViewers(VariationPopup popup) {
        if (!Platform.hasPacketEvents() || popup == null) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(popup.getVirtualId());
        for (UUID uid : popup.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendPacket(p, destroy);
            }
        }
        popup.getViewers().clear();
    }

    private void clearVisualsForPlayer(Player player) {
        if (player == null || !Platform.hasPacketEvents()) return;
        UUID pUid = player.getUniqueId();
        for (HealthIndicator ind : activeIndicators.values()) {
            if (ind.removeViewer(pUid)) {
                destroyIndicatorForPlayer(player, ind.getVirtualIndicatorId());
            }
        }
        for (VariationPopup popup : activePopups.values()) {
            if (popup.removeViewer(pUid)) {
                destroyIndicatorForPlayer(player, popup.getVirtualId());
            }
        }
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

    public boolean isEligibleType(LivingEntity entity) {
        if (entity == null) return false;

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

    public boolean isEligible(LivingEntity entity) {
        if (entity == null || !entity.isValid() || entity.isDead() || entity.getHealth() <= 0) return false;
        return isEligibleType(entity);
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
        return indicatorIdCounter.incrementAndGet();
    }

    public int getActiveIndicatorCount() {
        return activeIndicators.size();
    }

    public int getActivePopupCount() {
        return activePopups.size();
    }

    public int getTrackedViewersCount() {
        Set<UUID> all = new HashSet<>();
        for (HealthIndicator ind : activeIndicators.values()) {
            all.addAll(ind.getViewers());
        }
        for (VariationPopup popup : activePopups.values()) {
            all.addAll(popup.getViewers());
        }
        return all.size();
    }
}
