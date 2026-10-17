package me.lhawk.mobvitals.death;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import me.lhawk.mobvitals.config.PluginConfig;
import me.lhawk.mobvitals.util.Platform;
import me.lhawk.mobvitals.util.Schedulers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CorpseManager {
    public static final class CorpseSession {
        private final int entityId;
        private final Location location;
        private final DeathProfile profile;
        private final double entityWidth;
        private final double entityHeight;
        private volatile long expireTimeMillis;
        private final Set<UUID> viewers;
        private int tickCounter;

        public CorpseSession(int entityId, Location location, DeathProfile profile, double entityWidth, double entityHeight, long expireTimeMillis, Set<UUID> viewers) {
            this.entityId = entityId;
            this.location = location;
            this.profile = profile;
            this.entityWidth = entityWidth;
            this.entityHeight = entityHeight;
            this.expireTimeMillis = expireTimeMillis;
            this.viewers = viewers;
            this.tickCounter = 0;
        }

        public int getEntityId() {
            return entityId;
        }

        public Location getLocation() {
            return location;
        }

        public DeathProfile getProfile() {
            return profile;
        }

        public double getEntityWidth() {
            return entityWidth;
        }

        public double getEntityHeight() {
            return entityHeight;
        }

        public long getExpireTimeMillis() {
            return expireTimeMillis;
        }

        public void setExpireTimeMillis(long expireTimeMillis) {
            this.expireTimeMillis = expireTimeMillis;
        }

        public Set<UUID> getViewers() {
            return viewers;
        }

        public int getTickCounter() {
            return tickCounter;
        }

        public void incrementTick() {
            this.tickCounter++;
        }
    }

    private final Plugin plugin;
    private final PluginConfig config;
    private final Map<Integer, CorpseSession> activeCorpses = new ConcurrentHashMap<>();
    private final Set<Integer> delayedDestroyIds = ConcurrentHashMap.newKeySet();
    private Schedulers.TaskHandle tickTask;

    public CorpseManager(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();
        long period = config.getAmbientParticleRateTicks();
        tickTask = Schedulers.timer(plugin, 2L, period, this::tick);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        delayedDestroyIds.clear();
        for (CorpseSession session : activeCorpses.values()) {
            sendDestroyToViewers(session);
        }
        activeCorpses.clear();
    }

    public void removeCorpse(int entityId) {
        delayedDestroyIds.remove(entityId);
        CorpseSession session = activeCorpses.remove(entityId);
        if (session != null) {
            sendDestroyToViewers(session);
        }
    }

    public boolean isDelayedDestroy(int entityId) {
        return delayedDestroyIds.contains(entityId);
    }

    public void onEntityDeath(Entity entity) {
        if (!config.isDeathAnimationEnabled() || entity == null) return;
        if (entity instanceof Player && !config.isEnableForPlayers()) return;

        int entityId = entity.getEntityId();
        Location loc = entity.getLocation();
        if (loc.getWorld() == null) return;

        DeathProfile profile = DeathProfile.fromEntity(entity);
        long durationMs = (long) (config.getCorpseDurationSeconds() * 1000.0);
        long now = System.currentTimeMillis();

        if (config.isDenseCorpseReduction()) {
            double radiusSq = config.getDenseRadius() * config.getDenseRadius();
            List<CorpseSession> cluster = new ArrayList<>();
            for (CorpseSession s : activeCorpses.values()) {
                if (s.getLocation().getWorld() == loc.getWorld() && s.getLocation().distanceSquared(loc) <= radiusSq) {
                    cluster.add(s);
                }
            }
            int count = cluster.size();
            if (count > 0) {
                double factor;
                if (count == 1) {
                    factor = 0.70;
                } else if (count == 2) {
                    factor = 0.50;
                } else if (count == 3) {
                    factor = 0.35;
                } else {
                    factor = Math.max(0.15, 1.0 / (count + 1));
                }
                long scaled = Math.max(800L, (long) (durationMs * factor));
                durationMs = scaled;
                for (CorpseSession neighbor : cluster) {
                    long remaining = neighbor.getExpireTimeMillis() - now;
                    if (remaining > scaled) {
                        neighbor.setExpireTimeMillis(now + scaled);
                    }
                }
            }
        }

        long expireTime = now + durationMs;

        double viewDistSq = config.getViewDistance() * config.getViewDistance();
        Set<UUID> viewers = new HashSet<>();
        for (Player p : loc.getWorld().getPlayers()) {
            if (entity instanceof Player && p.getUniqueId().equals(entity.getUniqueId())) continue;
            if (p.getLocation().distanceSquared(loc) <= viewDistSq) {
                viewers.add(p.getUniqueId());
            }
        }

        if (viewers.isEmpty()) return;

        boolean isPlayer = entity instanceof Player;
        if (config.isEnableCorpseBody() && !isPlayer) {
            delayedDestroyIds.add(entityId);
        }

        double width = Math.max(0.3, entity.getWidth());
        double height = Math.max(0.3, entity.getHeight());
        CorpseSession session = new CorpseSession(entityId, loc.clone(), profile, width, height, expireTime, viewers);
        activeCorpses.put(entityId, session);

        if (config.isEnableCorpseBody() && !isPlayer) {
            sendCorpseStateToViewers(session);
        }

        if (config.isEnableSoundEffects()) {
            for (UUID vid : viewers) {
                Player vp = plugin.getServer().getPlayer(vid);
                if (vp != null && vp.isOnline()) {
                    vp.playSound(loc, profile.getAmbientSound(), 0.5f, profile.getAmbientPitch());
                }
            }
        }
    }

    private void tick() {
        if (activeCorpses.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, CorpseSession> entry : activeCorpses.entrySet()) {
            int entityId = entry.getKey();
            CorpseSession session = entry.getValue();

            Location loc = session.getLocation();
            World world = loc.getWorld();
            if (world == null || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                delayedDestroyIds.remove(entityId);
                activeCorpses.remove(entityId);
                sendDestroyToViewers(session);
                continue;
            }

            if (now >= session.getExpireTimeMillis()) {
                activeCorpses.remove(entityId);
                finishCorpse(session);
                continue;
            }

            double viewDistSq = config.getViewDistance() * config.getViewDistance();
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.getEntityId() == entityId) continue;
                if (p.getLocation().distanceSquared(loc) <= viewDistSq) {
                    if (session.getViewers().add(p.getUniqueId())) {
                        if (config.isEnableCorpseBody() && !session.getProfile().isPlayer()) {
                            sendCorpseStateToPlayer(p, session);
                        }
                    }
                }
            }

            session.incrementTick();
            if (config.isEnableAmbientParticles()) {
                renderAmbient(session);
            }
        }
    }

    private void renderAmbient(CorpseSession session) {
        Location loc = session.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        DeathProfile profile = session.getProfile();
        int step = session.getTickCounter();

        double yawRad = Math.toRadians(loc.getYaw());
        double shift = Math.max(0.35, Math.min(0.65, session.getEntityHeight() * 0.42));
        double cx = loc.getX() + Math.cos(yawRad) * shift;
        double cz = loc.getZ() + Math.sin(yawRad) * shift;
        double cy = loc.getY() + Math.max(0.12, Math.min(0.28, session.getEntityWidth() * 0.35));

        String[] particles = profile.getAmbientParticles();
        if (particles.length > 0) {
            String pName = particles[step % particles.length];
            sendParticleToViewers(session, pName, cx, cy, cz, 1, 0.05f, 0.04f, 0.05f, 0.01f);
        }

        if (particles.length > 1) {
            String second = particles[(step + 1) % particles.length];
            sendParticleToViewers(session, second, cx, cy, cz, 1, 0.04f, 0.03f, 0.04f, 0.01f);
        }

        if (step % 30 == 0 && config.isEnableSoundEffects()) {
            for (UUID uid : session.getViewers()) {
                Player p = plugin.getServer().getPlayer(uid);
                if (p != null && p.isOnline()) {
                    p.playSound(loc, profile.getAmbientSound(), 0.25f, profile.getAmbientPitch() + 0.1f);
                }
            }
        }
    }

    private void finishCorpse(CorpseSession session) {
        int entityId = session.getEntityId();
        delayedDestroyIds.remove(entityId);

        Location loc = session.getLocation();
        DeathProfile profile = session.getProfile();
        double radius = Math.max(0.25, Math.min(0.9, session.getEntityWidth() * 0.55));
        double yawRad = Math.toRadians(loc.getYaw());
        double shift = Math.max(0.35, Math.min(0.65, session.getEntityHeight() * 0.42));
        double cx = loc.getX() + Math.cos(yawRad) * shift;
        double cz = loc.getZ() + Math.sin(yawRad) * shift;
        double cy = loc.getY() + Math.max(0.12, Math.min(0.28, session.getEntityWidth() * 0.35));

        if (config.isEnableFinalExplosion()) {
            String[] finale = profile.getFinaleParticles();
            for (String pName : finale) {
                for (int i = 0; i < 6; i++) {
                    double theta = (i * Math.PI) / 3.0;
                    double fx = cx + (radius * 1.4) * Math.cos(theta);
                    double fz = cz + (radius * 1.4) * Math.sin(theta);
                    double fy = cy + 0.12;
                    sendParticleToViewers(session, pName, fx, fy, fz, 2, 0.05f, 0.05f, 0.05f, 0.03f);
                }
                sendParticleToViewers(session, pName, cx, cy + 0.15, cz, 4, 0.1f, 0.15f, 0.1f, 0.03f);
            }
        }

        if (config.isEnableSoundEffects()) {
            for (UUID uid : session.getViewers()) {
                Player p = plugin.getServer().getPlayer(uid);
                if (p != null && p.isOnline()) {
                    p.playSound(loc, profile.getFinaleSound(), 0.7f, profile.getFinalePitch());
                }
            }
        }

        sendDestroyToViewers(session);
    }

    private void sendCorpseStateToViewers(CorpseSession session) {
        if (!Platform.hasPacketEvents()) return;
        for (UUID uid : session.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendCorpseStateToPlayer(p, session);
            }
        }
    }

    private void sendCorpseStateToPlayer(Player player, CorpseSession session) {
        if (!Platform.hasPacketEvents() || player == null || !player.isOnline()) return;
        int entityId = session.getEntityId();
        if (player.getEntityId() == entityId) return;
        if (session.getProfile().isPlayer()) return;

        int healthIndex = 8;
        try {
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) {
                healthIndex = 7;
            }
        } catch (Throwable ignored) {}

        List<EntityData<?>> metadata = new ArrayList<>(4);
        metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0));
        metadata.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
        metadata.add(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.DYING));
        metadata.add(new EntityData<>(healthIndex, EntityDataTypes.FLOAT, 0.0f));

        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        WrapperPlayServerEntityStatus statusPacket = new WrapperPlayServerEntityStatus(entityId, (byte) 3);

        try {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, metaPacket);
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, statusPacket);
        } catch (Throwable t) {
            plugin.getLogger().warning("Corpse state send error: " + t.getMessage());
        }
    }

    private void sendDestroyToViewers(CorpseSession session) {
        if (!Platform.hasPacketEvents() || session.getProfile().isPlayer()) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(session.getEntityId());

        Set<Player> targets = new HashSet<>();
        for (UUID uid : session.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) targets.add(p);
        }
        World world = session.getLocation().getWorld();
        if (world != null) {
            double viewDistSq = config.getViewDistance() * config.getViewDistance();
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distanceSquared(session.getLocation()) <= viewDistSq) {
                    targets.add(p);
                }
            }
        }

        for (Player p : targets) {
            if (p.getEntityId() != session.getEntityId()) {
                try {
                    PacketEvents.getAPI().getPlayerManager().sendPacketSilently(p, destroy);
                } catch (Throwable ignored) {}
            }
        }
    }

    public void sendParticleToViewers(CorpseSession session, String particleName, double x, double y, double z, int count, float offX, float offY, float offZ, float speed) {
        if (particleName == null || session == null) return;

        ParticleType<?> type = resolvePacketParticleType(particleName);
        Vector3d pos = new Vector3d(x, y, z);
        Vector3f off = new Vector3f(offX, offY, offZ);

        WrapperPlayServerParticle packet = null;
        if (Platform.hasPacketEvents() && type != null) {
            try {
                Particle<?> p = new Particle<>(type);
                packet = new WrapperPlayServerParticle(p, false, pos, off, speed, count);
            } catch (Throwable ignored) {}
        }

        org.bukkit.Particle bukkitParticle = resolveBukkitParticle(particleName);

        for (UUID uid : session.getViewers()) {
            Player player = plugin.getServer().getPlayer(uid);
            if (player == null || !player.isOnline()) continue;

            if (packet != null && Platform.hasPacketEvents()) {
                try {
                    PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, packet);
                    continue;
                } catch (Throwable ignored) {}
            }

            if (bukkitParticle != null) {
                try {
                    player.spawnParticle(bukkitParticle, x, y, z, count, offX, offY, offZ, speed);
                } catch (Throwable ignored) {}
            }
        }
    }

    public void playPreviewEffect(Player player, DeathProfile profile) {
        if (player == null || profile == null) return;
        Location loc = player.getLocation();
        Set<UUID> singleViewer = Collections.singleton(player.getUniqueId());
        CorpseSession previewSession = new CorpseSession(1500999999, loc, profile, 0.6, 1.8, System.currentTimeMillis() + 3000L, singleViewer);

        Schedulers.timer(plugin, 1L, 4L, new Runnable() {
            private int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks < 15) {
                    previewSession.incrementTick();
                    renderAmbient(previewSession);
                } else if (ticks == 15) {
                    finishCorpse(previewSession);
                }
            }
        });
    }

    private ParticleType<?> resolvePacketParticleType(String name) {
        if (name == null) return ParticleTypes.FLAME;
        String lower = name.toLowerCase(Locale.ROOT);
        try {
            ParticleType<?> p = ParticleTypes.getByName(lower);
            if (p != null) return p;
            return ParticleTypes.getByName("minecraft:" + lower);
        } catch (Throwable ignored) {
            return ParticleTypes.FLAME;
        }
    }

    private org.bukkit.Particle resolveBukkitParticle(String name) {
        if (name == null) return org.bukkit.Particle.FLAME;
        try {
            return org.bukkit.Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return org.bukkit.Particle.FLAME;
        }
    }

    public int getActiveCorpseCount() {
        return activeCorpses.size();
    }
}
