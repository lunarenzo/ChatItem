package io.github.lunatech.chatitem.inventory;

import io.github.lunatech.chatitem.ChatItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InventoryListener implements Listener {
    private final ChatItem plugin;

    public InventoryListener(ChatItem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SnapshotInventoryHolder ||
            event.getInventory().getHolder() instanceof EnderChestInventoryHolder ||
            event.getInventory().getHolder() instanceof ShulkerInventoryHolder) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 5L);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SnapshotInventoryHolder ||
            event.getInventory().getHolder() instanceof EnderChestInventoryHolder ||
            event.getInventory().getHolder() instanceof ShulkerInventoryHolder) {
            event.setCancelled(true);
        }
    }
}
