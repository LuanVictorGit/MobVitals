package me.lhawk.mobvitals.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class Schedulers {
    public interface TaskHandle {
        void cancel();
    }

    private Schedulers() {}

    public static boolean folia() {
        return Platform.isFolia();
    }

    public static void run(Plugin plugin, Runnable task) {
        if (folia()) {
            try {
                Object gs = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(Bukkit.getServer());
                Method m = gs.getClass().getMethod("execute", Plugin.class, Runnable.class);
                m.invoke(gs, plugin, task);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (folia()) {
            try {
                Object as = Bukkit.getServer().getClass().getMethod("getAsyncScheduler").invoke(Bukkit.getServer());
                Method m = as.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                m.invoke(as, plugin, (Consumer<Object>) t -> task.run());
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void runAt(Plugin plugin, Location loc, Runnable task) {
        if (folia() && loc != null && loc.getWorld() != null) {
            try {
                Object rs = Bukkit.getServer().getClass().getMethod("getRegionScheduler").invoke(Bukkit.getServer());
                Method m = rs.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class);
                m.invoke(rs, plugin, loc, task);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (folia() && entity != null) {
            try {
                Method getScheduler = entity.getClass().getMethod("getScheduler");
                Object es = getScheduler.invoke(entity);
                Method m = es.getClass().getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
                m.invoke(es, plugin, task, null, 1L);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static TaskHandle later(Plugin plugin, long ticks, Runnable task) {
        if (folia()) {
            try {
                Object gs = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(Bukkit.getServer());
                Method m = gs.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                Object scheduled = m.invoke(gs, plugin, (Consumer<Object>) t -> task.run(), ticks);
                return () -> cancelScheduled(scheduled);
            } catch (Throwable ignored) {}
        }
        BukkitTask bt = Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
        return bt::cancel;
    }

    public static TaskHandle timer(Plugin plugin, long delayTicks, long periodTicks, Runnable task) {
        if (folia()) {
            try {
                Object gs = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(Bukkit.getServer());
                Method m = gs.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                Object scheduled = m.invoke(gs, plugin, (Consumer<Object>) t -> task.run(), delayTicks, periodTicks);
                return () -> cancelScheduled(scheduled);
            } catch (Throwable ignored) {}
        }
        BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return bt::cancel;
    }

    public static TaskHandle timerAsync(Plugin plugin, long delayTicks, long periodTicks, Runnable task) {
        if (folia()) {
            try {
                Object as = Bukkit.getServer().getClass().getMethod("getAsyncScheduler").invoke(Bukkit.getServer());
                Method m = as.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, java.util.concurrent.TimeUnit.class);
                long delayMs = Math.max(1, delayTicks * 50);
                long periodMs = Math.max(1, periodTicks * 50);
                Object scheduled = m.invoke(as, plugin, (Consumer<Object>) t -> task.run(), delayMs, periodMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                return () -> cancelScheduled(scheduled);
            } catch (Throwable ignored) {}
        }
        BukkitTask bt = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bt::cancel;
    }

    private static void cancelScheduled(Object scheduled) {
        if (scheduled == null) return;
        try {
            Method cancel = scheduled.getClass().getMethod("cancel");
            cancel.invoke(scheduled);
        } catch (Throwable ignored) {}
    }
}
