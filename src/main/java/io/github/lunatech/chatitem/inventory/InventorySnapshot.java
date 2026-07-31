package io.github.lunatech.chatitem.inventory;

import org.bukkit.inventory.ItemStack;

public record InventorySnapshot(
    String playerName,
    int expLevel,
    ItemStack[] items,
    long timestamp
) {}
