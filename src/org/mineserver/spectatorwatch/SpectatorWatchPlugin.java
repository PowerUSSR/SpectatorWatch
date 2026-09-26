package org.mineserver.spectatorwatch;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Ванильный Minecraft скрывает игроков в spectator-режиме от всех, кто сам не в spectator.
// Этот плагин показывает Админу/Заму/Основателю (право spectatorwatch.see) заметный маркер —
// голову и ник — над каждым, кто сейчас летает в spectator, чтобы это не было незаметно.
public class SpectatorWatchPlugin extends JavaPlugin implements Listener {

    private static final String PERMISSION = "spectatorwatch.see";
    private final Map<UUID, ArmorStand> markers = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 10L, 10L);
    }

    @Override
    public void onDisable() {
        for (ArmorStand stand : markers.values()) {
            if (stand != null && !stand.isDead()) stand.remove();
        }
        markers.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeMarker(event.getPlayer().getUniqueId());
    }

    private void tick() {
        for (Player spectator : getServer().getOnlinePlayers()) {
            if (spectator.getGameMode() == GameMode.SPECTATOR) {
                updateMarker(spectator);
            } else {
                removeMarker(spectator.getUniqueId());
            }
        }
        // Обновляем видимость на случай изменения прав/подключения новых зрителей
        for (ArmorStand stand : markers.values()) {
            applyVisibility(stand);
        }
    }

    private void updateMarker(Player spectator) {
        ArmorStand stand = markers.get(spectator.getUniqueId());
        if (stand == null || stand.isDead()) {
            stand = spawnMarker(spectator);
            markers.put(spectator.getUniqueId(), stand);
        }
        Location loc = spectator.getLocation().clone().subtract(0, 1.6, 0);
        stand.teleport(loc);
        applyVisibility(stand);
    }

    private ArmorStand spawnMarker(Player spectator) {
        Location loc = spectator.getLocation().clone().subtract(0, 1.6, 0);
        ArmorStand stand = (ArmorStand) spectator.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setPersistent(false);
        stand.setCustomName("\u00a7f\u00a7l" + spectator.getName() + " \u00a77(spectator)");
        stand.setCustomNameVisible(true);
        stand.setGlowing(true);

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(spectator);
            skull.setItemMeta(meta);
        }
        stand.getEquipment().setHelmet(skull);
        return stand;
    }

    private void applyVisibility(ArmorStand stand) {
        for (Player viewer : getServer().getOnlinePlayers()) {
            if (viewer.hasPermission(PERMISSION) && !viewer.getUniqueId().equals(markerOwner(stand))) {
                viewer.showEntity(this, stand);
            } else {
                viewer.hideEntity(this, stand);
            }
        }
    }

    private UUID markerOwner(ArmorStand stand) {
        for (Map.Entry<UUID, ArmorStand> e : markers.entrySet()) {
            if (e.getValue().equals(stand)) return e.getKey();
        }
        return null;
    }

    private void removeMarker(UUID uuid) {
        ArmorStand stand = markers.remove(uuid);
        if (stand != null && !stand.isDead()) stand.remove();
    }
}
