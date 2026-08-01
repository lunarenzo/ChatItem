package io.github.lunatech.chatitem.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class EnderChestInventoryHolder implements InventoryHolder {
    private final EnderChestSnapshot snapshot;

    public EnderChestInventoryHolder(EnderChestSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public EnderChestSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
