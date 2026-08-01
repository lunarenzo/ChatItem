package io.github.lunatech.chatitem.inventory;

import org.bukkit.inventory.ItemStack;

public record ShulkerSnapshot(
    String playerName,
    ItemStack shulkerItem,
    ItemStack[] items,
    long timestamp
) {}
