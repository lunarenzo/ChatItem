package io.github.lunatech.chatitem.inventory;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.github.lunatech.chatitem.config.PluginConfig;
import io.github.lunatech.chatitem.utility.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class InventoryManager implements Reloadable {
    private final ChatItem plugin;
    
    // Thread-safe FIFO eviction cache using Collections.synchronizedMap and LinkedHashMap
    private final Map<String, InventorySnapshot> cache = Collections.synchronizedMap(
        new LinkedHashMap<String, InventorySnapshot>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, InventorySnapshot> eldest) {
                return size() > plugin.getConfigHandler().getConfig().inventoryShowcase.inventoryCacheMaxSize;
            }
        }
    );

    // Thread-safe FIFO eviction cache for ender chests
    private final Map<String, EnderChestSnapshot> enderCache = Collections.synchronizedMap(
        new LinkedHashMap<String, EnderChestSnapshot>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, EnderChestSnapshot> eldest) {
                return size() > plugin.getConfigHandler().getConfig().inventoryShowcase.inventoryCacheMaxSize;
            }
        }
    );
    
    private BukkitTask cleanupTask;

    public InventoryManager(ChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable(ChatItem plugin) {
        // Run cleanup task asynchronously every 30 seconds to clean expired snapshots
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            long ttlMs = plugin.getConfigHandler().getConfig().inventoryShowcase.inventorySnapshotTtlSeconds * 1000L;
            synchronized (cache) {
                cache.values().removeIf(snapshot -> now - snapshot.timestamp() > ttlMs);
            }
            synchronized (enderCache) {
                enderCache.values().removeIf(snapshot -> now - snapshot.timestamp() > ttlMs);
            }
        }, 600L, 600L);
    }

    @Override
    public void onDisable(ChatItem plugin) {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        cache.clear();
        enderCache.clear();
    }

    @Override
    public void onReload(ChatItem plugin) {
        cache.clear();
        enderCache.clear();
    }

    /**
     * Creates an immutable inventory snapshot on the main thread.
     *
     * @param player the player to snapshot
     * @return the created InventorySnapshot
     */
    public InventorySnapshot createSnapshot(Player player) {
        ItemStack[] items = new ItemStack[54];

        // 1. Create decorative grey stained glass separating pane
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.empty());
            pane.setItemMeta(paneMeta);
        }

        // Fill background placeholder slots
        items[2] = pane;
        items[7] = pane;
        for (int i = 9; i <= 17; i++) {
            items[i] = pane;
        }

        // 2. Player Head (Slot 0) - Using setPlayerProfile to embed textures directly for 100% offline-safety
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            PlayerProfile profile = player.getPlayerProfile();
            skullMeta.setPlayerProfile(profile);
            String titleFormat = plugin.getConfigHandler().getConfig().messages.invTitle;
            Component nameComp = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                titleFormat.replace("{player}", player.getName())
            );
            skullMeta.displayName(nameComp);
            head.setItemMeta(skullMeta);
        }
        items[0] = head;

        // 3. Experience Bottle (Slot 1)
        ItemStack exp = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta expMeta = exp.getItemMeta();
        if (expMeta != null) {
            expMeta.displayName(Component.text("Experience Level: " + player.getLevel(), NamedTextColor.YELLOW));
            exp.setItemMeta(expMeta);
        }
        items[1] = exp;

        // 4. Armor (Slots 3-6)
        ItemStack helmet = player.getInventory().getHelmet();
        items[3] = helmet != null ? helmet.clone() : null;

        ItemStack chestplate = player.getInventory().getChestplate();
        items[4] = chestplate != null ? chestplate.clone() : null;

        ItemStack leggings = player.getInventory().getLeggings();
        items[5] = leggings != null ? leggings.clone() : null;

        ItemStack boots = player.getInventory().getBoots();
        items[6] = boots != null ? boots.clone() : null;

        // 5. Off-hand (Slot 8)
        ItemStack offhand = player.getInventory().getItemInOffHand();
        items[8] = offhand != null && offhand.getType() != Material.AIR ? offhand.clone() : null;

        // 6. Main Inventory (Slots 18-44) -> maps from player inventory indices 9-35
        for (int i = 9; i <= 35; i++) {
            ItemStack item = player.getInventory().getItem(i);
            items[i + 9] = item != null && item.getType() != Material.AIR ? item.clone() : null;
        }

        // 7. Hotbar (Slots 45-53) -> maps from player inventory indices 0-8
        for (int i = 0; i <= 8; i++) {
            ItemStack item = player.getInventory().getItem(i);
            items[i + 45] = item != null && item.getType() != Material.AIR ? item.clone() : null;
        }

        return new InventorySnapshot(player.getName(), player.getLevel(), items, System.currentTimeMillis());
    }

    /**
     * Registers a snapshot in the cache and generates a token for it.
     * Enforces a hard FIFO cache capacity constraint.
     *
     * @param snapshot the snapshot to cache
     * @return the generated token
     */
    public String registerSnapshot(InventorySnapshot snapshot) {
        String token = Util.randomString(8);
        cache.put(token, snapshot);
        return token;
    }

    /**
     * Opens the virtual inventory preview for a player.
     *
     * @param viewer the player viewing the inventory
     * @param token  the snapshot token
     * @return true if opened successfully, false if expired or invalid
     */
    public boolean openInventory(Player viewer, String token) {
        InventorySnapshot snapshot = cache.get(token);
        long ttlMs = plugin.getConfigHandler().getConfig().inventoryShowcase.inventorySnapshotTtlSeconds * 1000L;
        if (snapshot == null || System.currentTimeMillis() - snapshot.timestamp() > ttlMs) {
            if (snapshot != null) {
                cache.remove(token);
            }
            return false;
        }

        String titleFormat = plugin.getConfigHandler().getConfig().messages.invTitle;
        Component titleComp = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
            titleFormat.replace("{player}", snapshot.playerName())
        );

        Inventory inv = Bukkit.createInventory(new SnapshotInventoryHolder(snapshot), 54, titleComp);
        inv.setContents(snapshot.items());
        viewer.openInventory(inv);
        return true;
    }

    /**
     * Creates an immutable snapshot of a player's ender chest.
     *
     * @param player the player
     * @return the created EnderChestSnapshot
     */
    public EnderChestSnapshot createEnderChestSnapshot(Player player) {
        Inventory ender = player.getEnderChest();
        int size = ender.getSize();
        ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            ItemStack item = ender.getItem(i);
            items[i] = item != null && item.getType() != Material.AIR ? item.clone() : null;
        }
        return new EnderChestSnapshot(player.getName(), items, System.currentTimeMillis());
    }

    /**
     * Registers an ender chest snapshot in the cache.
     *
     * @param snapshot the snapshot to cache
     * @return the generated token
     */
    public String registerEnderChestSnapshot(EnderChestSnapshot snapshot) {
        String token = Util.randomString(8);
        enderCache.put(token, snapshot);
        return token;
    }

    /**
     * Opens the virtual ender chest preview for a player.
     *
     * @param viewer the player viewing the ender chest
     * @param token  the snapshot token
     * @return true if opened successfully, false if expired or invalid
     */
    public boolean openEnderChest(Player viewer, String token) {
        EnderChestSnapshot snapshot = enderCache.get(token);
        long ttlMs = plugin.getConfigHandler().getConfig().inventoryShowcase.inventorySnapshotTtlSeconds * 1000L;
        if (snapshot == null || System.currentTimeMillis() - snapshot.timestamp() > ttlMs) {
            if (snapshot != null) {
                enderCache.remove(token);
            }
            return false;
        }

        String titleFormat = plugin.getConfigHandler().getConfig().messages.enderTitle;
        Component titleComp = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
            titleFormat.replace("{player}", snapshot.playerName())
        );

        Inventory inv = Bukkit.createInventory(new EnderChestInventoryHolder(snapshot), snapshot.items().length, titleComp);
        inv.setContents(snapshot.items());
        viewer.openInventory(inv);
        return true;
    }
}
