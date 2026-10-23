package me.lhawk.mobvitals.listener;

import me.lhawk.mobvitals.death.CorpseManager;
import me.lhawk.mobvitals.indicator.IndicatorManager;
import me.lhawk.mobvitals.util.Schedulers;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public final class EntityVitalListener implements Listener {
    private final Plugin plugin;
    private final IndicatorManager indicatorManager;
    private final CorpseManager corpseManager;

    public EntityVitalListener(Plugin plugin, IndicatorManager indicatorManager, CorpseManager corpseManager) {
        this.plugin = plugin;
        this.indicatorManager = indicatorManager;
        this.corpseManager = corpseManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            double finalDamage = event.getFinalDamage();
            if (finalDamage > 0.0) {
                indicatorManager.onDamage(living, finalDamage);
            }
            Schedulers.runForEntity(plugin, living, () -> {
                if (living.isValid() && !living.isDead()) {
                    indicatorManager.updateIndicatorHealth(living);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            double amount = event.getAmount();
            if (amount > 0.0) {
                indicatorManager.onHeal(living, amount);
            }
            Schedulers.runForEntity(plugin, living, () -> {
                if (living.isValid() && !living.isDead()) {
                    indicatorManager.updateIndicatorHealth(living);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity living = event.getEntity();
        indicatorManager.handleEntityDeath(living);
        corpseManager.onEntityDeath(living);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        int entityId = player.getEntityId();
        corpseManager.removeCorpse(entityId);
        indicatorManager.removeEntity(entityId);
        Schedulers.runForEntity(plugin, player, 2L, () -> {
            if (player.isOnline()) {
                indicatorManager.updateIndicatorHealth(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        int entityId = event.getPlayer().getEntityId();
        corpseManager.removeCorpse(entityId);
        indicatorManager.onPlayerQuit(event.getPlayer());
    }
}
