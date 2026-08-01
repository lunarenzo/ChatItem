package io.github.lunatech.chatitem.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ShulkerInventoryHolder implements InventoryHolder {
    private final ShulkerSnapshot snapshot;

    public ShulkerInventoryHolder(ShulkerSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public ShulkerSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
