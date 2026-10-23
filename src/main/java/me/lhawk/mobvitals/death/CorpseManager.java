package me.lhawk.mobvitals.death;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.EntityPositionData;
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
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import me.lhawk.mobvitals.config.PluginConfig;
import me.lhawk.mobvitals.util.GladiatorHook;
import me.lhawk.mobvitals.util.Platform;
import me.lhawk.mobvitals.util.Schedulers;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class CorpseManager {
    public static final class CorpseSession {
        private final int entityId;
        private final UUID entityUuid;
        private final Location location;
        private final DeathProfile profile;
        private final double entityWidth;
        private final double entityHeight;
        private volatile long expireTimeMillis;
        private final Set<UUID> viewers;
        private final boolean body;
        private int tickCounter;
        private volatile Schedulers.TaskHandle task;

        public CorpseSession(int entityId, UUID entityUuid, Location location, DeathProfile profile, double entityWidth, double entityHeight, long expireTimeMillis, Set<UUID> viewers, boolean body) {
            this.entityId = entityId;
            this.entityUuid = entityUuid;
            this.location = location;
            this.profile = profile;
            this.entityWidth = entityWidth;
            this.entityHeight = entityHeight;
            this.expireTimeMillis = expireTimeMillis;
            this.viewers = viewers;
            this.body = body;
            this.tickCounter = 0;
        }

        public int getEntityId() {
            return entityId;
        }

        public UUID getEntityUuid() {
            return entityUuid;
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

        public boolean isBody() {
            return body;
        }

        public int getTickCounter() {
            return tickCounter;
        }

        public void incrementTick() {
            this.tickCounter++;
        }

        public void setTask(Schedulers.TaskHandle task) {
            this.task = task;
        }

        public void cancelTask() {
            Schedulers.TaskHandle current = task;
            task = null;
            if (current != null) current.cancel();
        }
    }

    private static final String LEGACY_TEAM = "mv_corpse";

    private final Plugin plugin;
    private final PluginConfig config;
    private final Map<Integer, CorpseSession> activeCorpses = new ConcurrentHashMap<>();
    private final Set<Integer> delayedDestroyIds = ConcurrentHashMap.newKeySet();

    public CorpseManager(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();
        removeLegacyTeam();
    }

    public void stop() {
        delayedDestroyIds.clear();
        for (CorpseSession session : activeCorpses.values()) {
            session.cancelTask();
            sendDestroyToViewers(session);
        }
        activeCorpses.clear();
    }

    private void removeLegacyTeam() {
        try {
            Team team = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeam(LEGACY_TEAM);
            if (team != null) team.unregister();
        } catch (Throwable ignored) {}
    }

    public void removeCorpse(int entityId) {
        delayedDestroyIds.remove(entityId);
        CorpseSession session = activeCorpses.remove(entityId);
        if (session != null) {
            session.cancelTask();
            sendDestroyToViewers(session);
        }
    }

    public boolean hasCorpseBodies() {
        return !delayedDestroyIds.isEmpty();
    }

    public boolean isDelayedDestroyFor(int entityId, UUID viewer) {
        if (viewer == null || !delayedDestroyIds.contains(entityId)) return false;
        CorpseSession session = activeCorpses.get(entityId);
        return session != null && session.getViewers().contains(viewer);
    }

    public void onEntityDeath(Entity entity) {
        if (!config.isDeathAnimationEnabled() || entity == null) return;
        boolean isPlayer = entity instanceof Player;
        if (isPlayer && !config.isEnableForPlayers()) return;
        if (GladiatorHook.isGladiator(entity)) return;

        int entityId = entity.getEntityId();
        UUID entityUuid = entity.getUniqueId();
        Location deathLoc = entity.getLocation();
        World world = deathLoc.getWorld();
        if (world == null) return;
        boolean body = config.isEnableCorpseBody() && !isPlayer;
        Location loc = body ? groundBelow(deathLoc) : deathLoc.clone();

        DeathProfile profile = DeathProfile.fromEntity(entity);
        long durationMs = (long) (config.getCorpseDurationSeconds() * 1000.0);
        long now = System.currentTimeMillis();

        if (config.isDenseCorpseReduction()) {
            double radiusSq = config.getDenseRadius() * config.getDenseRadius();
            List<CorpseSession> cluster = new ArrayList<>();
            for (CorpseSession s : activeCorpses.values()) {
                if (s.getLocation().getWorld() == world && s.getLocation().distanceSquared(loc) <= radiusSq) {
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
        Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        for (Player p : world.getPlayers()) {
            if (p.getUniqueId().equals(entityUuid)) continue;
            if (p.getLocation().distanceSquared(loc) <= viewDistSq) {
                viewers.add(p.getUniqueId());
            }
        }

        if (viewers.isEmpty()) return;

        if (body) {
            delayedDestroyIds.add(entityId);
            if (entity instanceof LivingEntity living) {
                try {
                    living.setCollidable(false);
                } catch (Throwable ignored) {}
            }
        }

        double width = Math.max(0.3, entity.getWidth());
        double height = Math.max(0.3, entity.getHeight());
        CorpseSession session = new CorpseSession(entityId, entityUuid, loc, profile, width, height, expireTime, viewers, body);
        activeCorpses.put(entityId, session);
        if (body) {
            sendCorpseStateToViewers(session);
        }
        session.setTask(Schedulers.timerAt(plugin, loc, 1L, config.getAmbientParticleRateTicks(), () -> tick(session)));

        if (config.isEnableSoundEffects()) {
            for (UUID vid : viewers) {
                playSound(plugin.getServer().getPlayer(vid), loc, profile.getAmbientSound(), 0.5f, profile.getAmbientPitch());
            }
        }
    }

    private static Location groundBelow(Location deathLoc) {
        Location ground = deathLoc.clone();
        World world = deathLoc.getWorld();
        if (world == null) return ground;
        try {
            RayTraceResult hit = world.rayTraceBlocks(deathLoc.clone().add(0.0, 0.1, 0.0), new Vector(0, -1, 0), 8.0,
                    FluidCollisionMode.NEVER, true);
            if (hit != null) {
                ground.setY(hit.getHitPosition().getY());
            }
        } catch (Throwable ignored) {}
        return ground;
    }

    private void tick(CorpseSession session) {
        int entityId = session.getEntityId();
        if (activeCorpses.get(entityId) != session) {
            session.cancelTask();
            return;
        }

        try {
            Location loc = session.getLocation();
            World world = loc.getWorld();
            if (world == null || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                removeCorpse(entityId);
                return;
            }

            if (System.currentTimeMillis() >= session.getExpireTimeMillis()) {
                activeCorpses.remove(entityId, session);
                session.cancelTask();
                finishCorpse(session);
                return;
            }

            session.incrementTick();
            if (session.isBody() && session.getTickCounter() <= 2) {
                sendCorpseStateToViewers(session);
            }

            double viewDistSq = config.getViewDistance() * config.getViewDistance();
            for (Player p : world.getPlayers()) {
                if (p.getUniqueId().equals(session.getEntityUuid())) continue;
                if (p.getLocation().distanceSquared(loc) <= viewDistSq && session.getViewers().add(p.getUniqueId())) {
                    sendCorpseStateToPlayer(p, session);
                }
            }

            if (config.isEnableAmbientParticles()) {
                renderAmbient(session);
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to update a corpse, removing it", t);
            removeCorpse(entityId);
        }
    }

    private void sendCorpseStateToViewers(CorpseSession session) {
        for (UUID uid : session.getViewers()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendCorpseStateToPlayer(p, session);
            }
        }
    }

    private void sendCorpseStateToPlayer(Player player, CorpseSession session) {
        if (!Platform.hasPacketEvents() || player == null || !player.isOnline() || !session.isBody()) return;
        if (player.getUniqueId().equals(session.getEntityUuid())) return;
        int entityId = session.getEntityId();

        int healthIndex = 9;
        try {
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) {
                healthIndex = 8;
            }
        } catch (Throwable ignored) {}

        List<EntityData<?>> metadata = new ArrayList<>(4);
        metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0));
        metadata.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
        metadata.add(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.SLEEPING));
        metadata.add(new EntityData<>(healthIndex, EntityDataTypes.FLOAT, 1.0f));

        try {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, new WrapperPlayServerEntityMetadata(entityId, metadata));
        } catch (Throwable t) {
            plugin.getLogger().warning("Corpse state send error: " + t.getMessage());
        }
        sendCorpsePositionToPlayer(player, session);
    }

    private void sendCorpsePositionToPlayer(Player player, CorpseSession session) {
        if (!Platform.hasPacketEvents() || player == null || !player.isOnline()) return;
        int entityId = session.getEntityId();
        Location loc = session.getLocation();
        Vector3d pos = new Vector3d(loc.getX(), loc.getY(), loc.getZ());
        WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(entityId, new Vector3d(0, 0, 0));

        try {
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2)) {
                EntityPositionData posData = new EntityPositionData(pos, Vector3d.zero(), loc.getYaw(), 0f);
                WrapperPlayServerEntityPositionSync sync = new WrapperPlayServerEntityPositionSync(entityId, posData, true);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, sync);
            }
        } catch (Throwable ignored) {}

        try {
            WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(entityId, pos, loc.getYaw(), 0f, true);
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, teleport);
        } catch (Throwable ignored) {}

        try {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, velocity);
        } catch (Throwable ignored) {}
    }

    private void renderAmbient(CorpseSession session) {
        Location loc = session.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        DeathProfile profile = session.getProfile();
        int step = session.getTickCounter();

        double cx = loc.getX();
        double cz = loc.getZ();
        double cy = loc.getY() + 0.15;

        String[] particles = profile.getAmbientParticles();
        if (particles.length > 0) {
            String pName = particles[step % particles.length];
            sendParticleToViewers(session, pName, cx, cy, cz, 1, 0.15f, 0.05f, 0.15f, 0.01f);
        }

        if (particles.length > 1) {
            String second = particles[(step + 1) % particles.length];
            sendParticleToViewers(session, second, cx, cy, cz, 1, 0.12f, 0.04f, 0.12f, 0.01f);
        }

        if (step % 30 == 0 && config.isEnableSoundEffects()) {
            for (UUID uid : session.getViewers()) {
                playSound(plugin.getServer().getPlayer(uid), loc, profile.getAmbientSound(), 0.25f, profile.getAmbientPitch() + 0.1f);
            }
        }
    }

    private void finishCorpse(CorpseSession session) {
        delayedDestroyIds.remove(session.getEntityId());

        Location loc = session.getLocation();
        DeathProfile profile = session.getProfile();
        double radius = Math.max(0.25, Math.min(0.9, session.getEntityWidth() * 0.55));
        double cx = loc.getX();
        double cz = loc.getZ();
        double cy = loc.getY() + 0.15;

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
                playSound(plugin.getServer().getPlayer(uid), loc, profile.getFinaleSound(), 0.7f, profile.getFinalePitch());
            }
        }

        sendDestroyToViewers(session);
    }

    private void sendDestroyToViewers(CorpseSession session) {
        if (!Platform.hasPacketEvents() || !session.isBody()) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(session.getEntityId());
        for (UUID uid : session.getViewers()) {
            if (uid.equals(session.getEntityUuid())) continue;
            Player p = plugin.getServer().getPlayer(uid);
            if (p == null || !p.isOnline()) continue;
            try {
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(p, destroy);
            } catch (Throwable ignored) {}
        }
    }

    private void playSound(Player player, Location loc, Sound sound, float volume, float pitch) {
        if (player == null || !player.isOnline() || sound == null || !Schedulers.ownsEntity(player)) return;
        try {
            player.playSound(loc, sound, volume, pitch);
        } catch (Throwable ignored) {}
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

            if (bukkitParticle != null && Schedulers.ownsEntity(player)) {
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
        CorpseSession previewSession = new CorpseSession(1500999999, UUID.randomUUID(), loc, profile, 0.6, 1.8, System.currentTimeMillis() + 3000L, singleViewer, false);

        Schedulers.TaskHandle[] task = new Schedulers.TaskHandle[1];
        task[0] = Schedulers.timerForEntity(plugin, player, 1L, 4L, new Runnable() {
            private int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks < 15) {
                    previewSession.incrementTick();
                    renderAmbient(previewSession);
                } else {
                    finishCorpse(previewSession);
                    if (task[0] != null) task[0].cancel();
                }
            }
        }, () -> {
            if (task[0] != null) task[0].cancel();
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
