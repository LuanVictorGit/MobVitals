package me.lhawk.mobvitals.util;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class GladiatorHook {
    private static final String PLUGIN_NAME = "ZombieGladiators";
    private static final NamespacedKey GLADIATOR_KEY = NamespacedKey.fromString("zombiegladiators:gladiator");

    private GladiatorHook() {}

    public static boolean isGladiator(Entity entity) {
        if (!(entity instanceof Zombie) || GLADIATOR_KEY == null) return false;
        Plugin zombieGladiators = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (zombieGladiators == null || !zombieGladiators.isEnabled()) return false;
        try {
            return entity.getPersistentDataContainer().has(GLADIATOR_KEY, PersistentDataType.BYTE);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
