package me.lhawk.mobvitals.indicator;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VariationPopup {
    private final int virtualId;
    private final Location location;
    private final Component component;
    private final long expireTimeMillis;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    public VariationPopup(int virtualId, Location location, Component component, long expireTimeMillis) {
        this.virtualId = virtualId;
        this.location = location;
        this.component = component;
        this.expireTimeMillis = expireTimeMillis;
    }

    public int getVirtualId() {
        return virtualId;
    }

    public Location getLocation() {
        return location;
    }

    public Component getComponent() {
        return component;
    }

    public long getExpireTimeMillis() {
        return expireTimeMillis;
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
