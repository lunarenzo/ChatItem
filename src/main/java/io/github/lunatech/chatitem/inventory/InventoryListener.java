package io.github.lunatech.chatitem.inventory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InventoryListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SnapshotInventoryHolder ||
            event.getInventory().getHolder() instanceof EnderChestInventoryHolder ||
            event.getInventory().getHolder() instanceof ShulkerInventoryHolder) {
            event.setCancelled(true);
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
