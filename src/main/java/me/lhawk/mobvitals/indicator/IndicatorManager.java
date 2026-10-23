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
import me.lhawk.mobvitals.util.GladiatorHook;
import me.lhawk.mobvitals.util.Platform;
import me.lhawk.mobvitals.util.Schedulers;
import me.lhawk.mobvitals.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
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
import java.util.logging.Level;

public final class IndicatorManager {
    public static final String HIDE_INDICATOR_KEY = "mobvitals-hide-indicator";
    public static final String HIDE_POPUPS_KEY = "mobvitals-hide-popups";
    public static final String NAME_KEY = "mobvitals-name";

    private final Plugin plugin;
    private final PluginConfig config;

    private final AtomicInteger indicatorIdCounter = new AtomicInteger(1_500_000_000);
    private final Map<Integer, HealthIndicator> activeIndicators = new ConcurrentHashMap<>();
    private final Map<Integer, VariationPopup> activePopups = new ConcurrentHashMap<>();
    private final Set<UUID> toggledOffPlayers = ConcurrentHashMap.newKeySet();

    private Schedulers.TaskHandle updateTask;

    public IndicatorManager(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();
        long interval = config.getUpdateIntervalTicks();
        updateTask = Schedulers.timer(plugin, 2L, interval, this::tickPopups);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (HealthIndicator indicator : activeIndicators.values()) {
            indicator.cancelTask();
            if (Platform.hasPacketEvents()) destroyForViewers(indicator);
        }
        if (Platform.hasPacketEvents()) {
            for (VariationPopup popup : activePopups.values()) {
                destroyPopupForViewers(popup);
            }
        }
        activeIndicators.clear();
        activePopups.clear();
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

    public void onDamage(LivingEntity entity, double finalDamage) {
        if (entity == null) return;
        boolean popup = config.isPopupsEnabled() && finalDamage >= config.getPopupThreshold() && canShowPopups(entity);
        boolean indicator = config.isIndicatorsEnabled() && canShowIndicator(entity);
        if (!popup && !indicator) return;

        if (popup) {
            spawnVariationPopup(entity, finalDamage, true);
        }

        double healthAfter = Math.max(0.0, entity.getHealth() - finalDamage);
        if (healthAfter > 0.0 && indicator) {
            showOrUpdateIndicator(entity, healthAfter, getMaxHealth(entity));
        }
    }

    public void onHeal(LivingEntity entity, double amount) {
        if (entity == null) return;
        boolean popups = config.isPopupsEnabled() && canShowPopups(entity);
        boolean indicator = config.isIndicatorsEnabled() && canShowIndicator(entity);
        if (!popups && !indicator) return;

        double currentHealth = Math.max(0.0, entity.getHealth());
        double maxHealth = getMaxHealth(entity);
        double healthAfter = Math.min(maxHealth, currentHealth + amount);
        double actualHeal = Math.max(0.0, healthAfter - currentHealth);

        if (popups && actualHeal >= config.getPopupThreshold()) {
            spawnVariationPopup(entity, actualHeal, false);
        }

        if (indicator) {
            showOrUpdateIndicator(entity, healthAfter, maxHealth);
        }
    }

    public void updateIndicatorHealth(LivingEntity entity) {
        if (!config.isIndicatorsEnabled() || entity == null || !isEligible(entity) || !canShowIndicator(entity)) return;
        showOrUpdateIndicator(entity, getEffectiveHealth(entity), getMaxHealth(entity));
    }

    public void showOrUpdateIndicator(LivingEntity entity, double currentHealth, double maxHealth) {
        if (!Platform.hasPacketEvents() || !config.isIndicatorsEnabled() || entity == null) return;

        int entityId = entity.getEntityId();
        HealthIndicator indicator = activeIndicators.get(entityId);
        if (indicator == null) {
            HealthIndicator created = new HealthIndicator(entity, getNextIndicatorId(), entity.getLocation(), currentHealth, maxHealth);
            activeIndicators.put(entityId, created);
            long interval = config.getUpdateIntervalTicks();
            created.setTask(Schedulers.timerForEntity(plugin, entity, interval, interval,
                    () -> tickIndicator(created), () -> removeIndicator(created)));
            updateIndicatorViewers(created, entity);
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
        removeEntity(entityId);
    }

    public void removeEntity(int entityId) {
        HealthIndicator indicator = activeIndicators.remove(entityId);
        if (indicator == null) return;
        indicator.cancelTask();
        if (Platform.hasPacketEvents()) destroyForViewers(indicator);
    }

    private void removeIndicator(HealthIndicator indicator) {
        activeIndicators.remove(indicator.getHostEntityId(), indicator);
        indicator.cancelTask();
        if (Platform.hasPacketEvents()) destroyForViewers(indicator);
    }

    private void tickPopups() {
        if (!Platform.hasPacketEvents()) return;
        long now = System.currentTimeMillis();
        activePopups.entrySet().removeIf(entry -> {
            VariationPopup popup = entry.getValue();
            try {
                if (now < popup.getExpireTimeMillis()) {
                    Location pLoc = popup.getLocation();
                    pLoc.add(0.0, 0.038, 0.0);
                    teleportPopupForViewers(popup, pLoc);
                    return false;
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to animate variation popup, removing it early", t);
            }
            destroyPopupForViewers(popup);
            return true;
        });
    }

    private void tickIndicator(HealthIndicator ind) {
        boolean remove;
        try {
            remove = !Platform.hasPacketEvents() || !config.isIndicatorsEnabled() || updateIndicator(ind);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to update health indicator, removing it", t);
            remove = true;
        }
        if (remove) {
            removeIndicator(ind);
        }
    }

    private boolean updateIndicator(HealthIndicator ind) {
        LivingEntity host = ind.getHostEntity();
        if (host == null || !host.isValid() || host.isDead() || host.getHealth() <= 0 || !canShowIndicator(host)) {
            return true;
        }

        long maxDamageAgeMs = (long) (config.getDamagedDisplaySeconds() * 1000.0);
        if (config.isShowOnlyWhenDamaged() && (System.currentTimeMillis() - ind.getLastDamageTime() > maxDamageAgeMs)) {
            return true;
        }

        Location currentLoc = host.getLocation();
        Location lastLoc = ind.getLastLocation();
        if (lastLoc != null && lastLoc.getWorld() != currentLoc.getWorld()) {
            destroyForViewers(ind);
            ind.setLastLocation(currentLoc.clone());
        } else if (lastLoc == null || lastLoc.distanceSquared(currentLoc) > 0.002) {
            teleportIndicatorForViewers(ind, host, currentLoc);
            ind.setLastLocation(currentLoc.clone());
        }

        double currentHealth = getEffectiveHealth(host);
        if (Math.abs(currentHealth - ind.getLastHealth()) > 0.05) {
            ind.setLastHealth(currentHealth);
            updateMetadataForViewers(ind, host);
        }

        updateIndicatorViewers(ind, host);
        return false;
    }

    public void spawnVariationPopup(LivingEntity entity, double amount, boolean isDamage) {
        if (!Platform.hasPacketEvents() || !config.isPopupsEnabled() || entity == null) return;

        Location eLoc = entity.getLocation();
        if (eLoc.getWorld() == null) return;

        int hostId = entity.getEntityId();
        makeRoomForPopup(hostId, isDamage);

        double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
        double dist = ThreadLocalRandom.current().nextDouble(0.25, 0.85);
        double offsetX = Math.cos(angle) * dist;
        double offsetZ = Math.sin(angle) * dist;
        double spawnY = eLoc.getY() + Math.max(0.55, entity.getHeight() * 0.70) + ThreadLocalRandom.current().nextDouble(-0.1, 0.35);

        Location popupLoc = new Location(eLoc.getWorld(), eLoc.getX() + offsetX, spawnY, eLoc.getZ() + offsetZ);
        String valStr = (amount == (long) amount) ? String.valueOf((long) amount) : String.format(Locale.ROOT, "%.1f", amount);
        String template = isDamage ? config.getDamageFormat() : config.getHealFormat();
        template = template.replace("<emerald>", "<green>").replace("</emerald>", "</green>");
        String formatted = template.replace("<value>", valStr);
        Component component = TextUtil.parse(formatted);

        int popupId = getNextIndicatorId();
        long expireTime = System.currentTimeMillis() + (long) (config.getPopupDurationSeconds() * 1000.0);
        VariationPopup popup = new VariationPopup(popupId, hostId, isDamage, popupLoc, component, expireTime);

        double viewDistSq = config.getViewDistance() * config.getViewDistance();
        for (Player p : eLoc.getWorld().getPlayers()) {
            if (!canView(p, entity)) continue;
            if (p.getLocation().distanceSquared(eLoc) <= viewDistSq) {
                if (popup.addViewer(p.getUniqueId())) {
                    spawnPopupForPlayer(p, popup);
                }
            }
        }

        activePopups.put(popupId, popup);
    }

    private void makeRoomForPopup(int hostId, boolean isDamage) {
        int max = config.getPopupMaxPerEntity();
        if (max <= 0) return;

        List<VariationPopup> same = new ArrayList<>();
        for (VariationPopup popup : activePopups.values()) {
            if (popup.getHostEntityId() == hostId && popup.isDamage() == isDamage) {
                same.add(popup);
            }
        }
        if (same.size() < max) return;

        same.sort(Comparator.comparingInt(VariationPopup::getVirtualId));
        for (int i = 0; i <= same.size() - max; i++) {
            VariationPopup oldest = same.get(i);
            if (activePopups.remove(oldest.getVirtualId(), oldest)) {
                destroyPopupForViewers(oldest);
            }
        }
    }

    private void updateIndicatorViewers(HealthIndicator indicator, LivingEntity host) {
        Location loc = host.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        indicator.getViewers().removeIf(uid -> {
            Player viewer = plugin.getServer().getPlayer(uid);
            return viewer == null || viewer.getWorld() != world;
        });

        double viewDistSq = config.getViewDistance() * config.getViewDistance();

        for (Player p : world.getPlayers()) {
            UUID pUid = p.getUniqueId();
            boolean inRange = canView(p, host) && p.getLocation().distanceSquared(loc) <= viewDistSq;
            if (inRange) {
                if (indicator.addViewer(pUid)) {
                    spawnIndicatorForPlayer(p, indicator, host, loc, indicator.getLastHealth(), indicator.getLastMaxHealth());
                }
            } else {
                if (indicator.removeViewer(pUid)) {
                    destroyIndicatorForPlayer(p, indicator.getVirtualIndicatorId());
                }
            }
        }
    }

    private boolean canView(Player viewer, LivingEntity host) {
        if (toggledOffPlayers.contains(viewer.getUniqueId())) return false;
        if (host instanceof Player hostPlayer) {
            return !hostPlayer.getUniqueId().equals(viewer.getUniqueId()) && viewer.canSee(hostPlayer);
        }
        return true;
    }

    private void spawnIndicatorForPlayer(Player player, HealthIndicator indicator, LivingEntity entity, Location loc, double health, double maxHealth) {
        if (!Platform.hasPacketEvents() || player == null || !player.isOnline()) return;
        if (entity instanceof Player hostPlayer && hostPlayer.getUniqueId().equals(player.getUniqueId())) return;

        Component comp = formatIndicator(entity, health, maxHealth);
        int virtualId = indicator.getVirtualIndicatorId();

        double yOffset = entity.getHeight() + config.getHeightOffset();
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
        Vector3d pos = new Vector3d(loc.getX(), loc.getY(), loc.getZ());

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
        double yOffset = entity.getHeight() + config.getHeightOffset();
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
        Vector3d pos = new Vector3d(loc.getX(), loc.getY(), loc.getZ());
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
        String json = TextUtil.toJson(component);
        metadata.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_COMPONENT, Optional.of(json)));
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
                .replace("<bar>", bar.toString())
                .replace("<color>", color)
                .replace("</color>", "<reset>")
                .replace("<current>", currentStr)
                .replace("<max>", maxStr)
                .replace("<percent>", percentStr);

        return TextUtil.parse(formatted, Placeholder.component("name", resolveName(entity)));
    }

    private Component resolveName(LivingEntity entity) {
        for (MetadataValue value : entity.getMetadata(NAME_KEY)) {
            String name = value.asString();
            if (name != null && !name.isEmpty()) return TextUtil.parseLenient(name);
        }
        if (entity instanceof Player) {
            return Component.text(entity.getName());
        }
        String customName = entity.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            return TextUtil.fromLegacy(customName);
        }
        return Component.text(formatEntityTypeName(entity.getType().name()));
    }

    public boolean canShowIndicator(LivingEntity entity) {
        return isEligibleType(entity) && isShown(entity) && !isMarked(entity, HIDE_INDICATOR_KEY);
    }

    public boolean canShowPopups(LivingEntity entity) {
        return isEligibleType(entity) && isShown(entity) && !isMarked(entity, HIDE_POPUPS_KEY);
    }

    private boolean isShown(LivingEntity entity) {
        if (entity.isInvisible()) return false;
        return !(entity instanceof Player player) || player.getGameMode() != GameMode.SPECTATOR;
    }

    private static boolean isMarked(LivingEntity entity, String key) {
        return entity.hasMetadata(key) || entity.getScoreboardTags().contains(key);
    }

    public boolean isEligibleType(LivingEntity entity) {
        if (entity == null) return false;

        String typeName = entity.getType().name().toUpperCase(Locale.ROOT);
        if (config.getBlacklist().contains(typeName)) return false;
        if (GladiatorHook.isGladiator(entity)) return false;

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
