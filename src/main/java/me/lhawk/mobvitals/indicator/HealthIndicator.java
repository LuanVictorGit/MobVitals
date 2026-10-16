package me.lhawk.mobvitals.indicator;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HealthIndicator {
    private final int hostEntityId;
    private final int virtualIndicatorId;
    private final WeakReference<LivingEntity> hostEntityRef;
    private Location lastLocation;
    private double lastHealth;
    private double lastMaxHealth;
    private long lastDamageTime;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    public HealthIndicator(LivingEntity hostEntity, int virtualIndicatorId, Location initialLocation, double health, double maxHealth) {
        this.hostEntityId = hostEntity != null ? hostEntity.getEntityId() : 0;
        this.virtualIndicatorId = virtualIndicatorId;
        this.hostEntityRef = new WeakReference<>(hostEntity);
        this.lastLocation = initialLocation;
        this.lastHealth = health;
        this.lastMaxHealth = maxHealth;
        this.lastDamageTime = System.currentTimeMillis();
    }

    public int getHostEntityId() {
        return hostEntityId;
    }

    public int getVirtualIndicatorId() {
        return virtualIndicatorId;
    }

    public LivingEntity getHostEntity() {
        return hostEntityRef.get();
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public double getLastHealth() {
        return lastHealth;
    }

    public void setLastHealth(double lastHealth) {
        this.lastHealth = lastHealth;
    }

    public double getLastMaxHealth() {
        return lastMaxHealth;
    }

    public void setLastMaxHealth(double lastMaxHealth) {
        this.lastMaxHealth = lastMaxHealth;
    }

    public long getLastDamageTime() {
        return lastDamageTime;
    }

    public void markDamaged() {
        this.lastDamageTime = System.currentTimeMillis();
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    public boolean addViewer(UUID uuid) {
        return viewers.add(uuid);
    }

    public boolean removeViewer(UUID uuid) {
        return viewers.remove(uuid);
    }
}
