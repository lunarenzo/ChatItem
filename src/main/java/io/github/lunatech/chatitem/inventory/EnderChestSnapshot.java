package io.github.lunatech.chatitem.inventory;

import org.bukkit.inventory.ItemStack;

public record EnderChestSnapshot(
    String playerName,
    ItemStack[] items,
    long timestamp
) {}
