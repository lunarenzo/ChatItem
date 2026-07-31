package io.github.lunatech.chatitem.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class SnapshotInventoryHolder implements InventoryHolder {
    private final InventorySnapshot snapshot;

    public SnapshotInventoryHolder(InventorySnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public InventorySnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
