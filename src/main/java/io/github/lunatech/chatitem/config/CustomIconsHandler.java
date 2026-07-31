package io.github.lunatech.chatitem.config;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.github.lunatech.chatitem.config.loading.ConfigLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomIconsHandler implements Reloadable, Listener {
    private final ChatItem plugin;
    private final Path configDir;
    private final Logger logger;

    private CustomIconsConfig cfg;
    private final Map<Material, CustomIconsConfig.IconOverride> overrideMap = new ConcurrentHashMap<>();
    private final Map<Material, Component> staticCache = new ConcurrentHashMap<>();
    private final Map<UUID, Component> playerFaceCache = new ConcurrentHashMap<>();
    private final Map<String, Component> skullFaceCache = new ConcurrentHashMap<>();
    private volatile long lastClearTime = System.currentTimeMillis();

    public CustomIconsHandler(ChatItem plugin) {
        this.plugin = plugin;
        this.configDir = plugin.getDataFolder().toPath();
        this.logger = plugin.getComponentLogger();
    }

    @Override
    public void onLoad(ChatItem plugin) {
        cfg = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("custom-icons.yml"))
            .withHeader("")
            .build(CustomIconsConfig.class);

        overrideMap.clear();
        staticCache.clear();
        playerFaceCache.clear();
        skullFaceCache.clear();
        lastClearTime = System.currentTimeMillis();

        Map<String, CustomIconsConfig.IconOverride> map = cfg.customIcons;
        if (map != null) {
            for (Map.Entry<String, CustomIconsConfig.IconOverride> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String matName = entry.getKey().trim().toUpperCase(Locale.ROOT);
                Material material;
                try {
                    material = Material.valueOf(matName);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid material ID specified in custom-icons.yml: '{}'. This entry will be ignored.", entry.getKey());
                    continue;
                }

                CustomIconsConfig.IconOverride override = entry.getValue();

                // Validate config override structure
                if ("player".equalsIgnoreCase(override.object)) {
                    boolean hasName = override.name != null && !override.name.isBlank();
                    boolean hasUuid = override.uuid != null && !override.uuid.isBlank();
                    boolean hasHash = override.hash != null && !override.hash.isBlank();
                    boolean hasValue = override.value != null && !override.value.isBlank();
                    if (!hasName && !hasUuid && !hasHash && !hasValue) {
                        logger.warn("Custom player icon for material '{}' has no identity properties (name, uuid, hash, or value). This entry will be ignored.", matName);
                        continue;
                    }
                } else if ("atlas".equalsIgnoreCase(override.object) || override.object == null) {
                    if (override.atlas == null || override.atlas.isBlank() || override.sprite == null || override.sprite.isBlank()) {
                        logger.warn("Custom atlas icon for material '{}' is missing required fields (atlas or sprite). This entry will be ignored.", matName);
                        continue;
                    }
                } else {
                    logger.warn("Custom icon for material '{}' has invalid object type: '{}'. Expected 'atlas' or 'player'. This entry will be ignored.", matName, override.object);
                    continue;
                }

                overrideMap.put(material, override);

                // Compile static cache if it doesn't depend on dynamic {player} placeholder
                if (isStatic(override)) {
                    Component component = buildComponent(override, null);
                    if (component != null) {
                        staticCache.put(material, component);
                    }
                }
            }
        }
    }

    @Override
    public void onEnable(ChatItem plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerFaceCache.remove(event.getPlayer().getUniqueId());
    }

    private boolean isStatic(CustomIconsConfig.IconOverride override) {
        if ("player".equalsIgnoreCase(override.object)) {
            return override.name == null || !override.name.contains("{player}");
        }
        return true;
    }

    private Component buildComponent(CustomIconsConfig.IconOverride override, String playerName) {
        try {
            if ("player".equalsIgnoreCase(override.object)) {
                String json;
                if (override.value != null && !override.value.isBlank()) {
                    json = "{\"object\":\"player\",\"player\":{\"properties\":[{\"name\":\"textures\",\"value\":\"" + override.value.trim() + "\"}]}}";
                } else if (override.hash != null && !override.hash.isBlank()) {
                    String texturesJson = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + override.hash.trim() + "\"}}}";
                    String base64Value = Base64.getEncoder().encodeToString(texturesJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    json = "{\"object\":\"player\",\"player\":{\"properties\":[{\"name\":\"textures\",\"value\":\"" + base64Value + "\"}]}}";
                } else if (override.uuid != null && !override.uuid.isBlank()) {
                    json = "{\"object\":\"player\",\"player\":{\"id\":\"" + override.uuid.trim() + "\"}}";
                } else {
                    String targetName = override.name;
                    if (targetName == null || targetName.isBlank()) {
                        targetName = playerName != null ? playerName : "Steve";
                    } else if (playerName != null) {
                        targetName = targetName.replace("{player}", playerName);
                    }
                    json = "{\"object\":\"player\",\"player\":\"" + targetName + "\"}";
                }
                return GsonComponentSerializer.gson().deserialize(json).append(Component.text(" "));
            } else {
                // Default: atlas type
                String iconFormat = plugin.getConfigHandler().getConfig().itemShowcase.iconFormat;
                String iconTag = iconFormat
                    .replace("{atlas}", override.atlas != null ? override.atlas : "minecraft:items")
                    .replace("{sprite}", override.sprite != null ? override.sprite : "item/barrier");
                return MiniMessage.miniMessage().deserialize(iconTag).color(NamedTextColor.WHITE);
            }
        } catch (Throwable e) {
            logger.error("Failed to build custom icon component", e);
            return null;
        }
    }

    /**
     * Resolves the custom icon component for the showcasing player.
     * Evaluates static cached components or dynamic player-dependent face components.
     *
     * @param material the Material type
     * @param player   the showcasing Player
     * @return the resolved Component, or null if no override is defined or if excluded
     */
    public Component getOverride(Material material, Player player) {
        if (material == null) return null;

        // If the item is in the excluded list, it will be ignored and remain hidden.
        if (plugin.getExcludedIconsHandler().isExcluded(material)) {
            return null;
        }

        Component cached = staticCache.get(material);
        if (cached != null) {
            return cached;
        }

        CustomIconsConfig.IconOverride override = overrideMap.get(material);
        if (override != null) {
            if (player != null && override.name != null && override.name.contains("{player}")) {
                return playerFaceCache.computeIfAbsent(player.getUniqueId(), uuid ->
                    buildComponent(override, player.getName())
                );
            }
            return buildComponent(override, player != null ? player.getName() : null);
        }

        return null;
    }

    /**
     * Resolves dynamic custom player skull metadata to a 2D face component with zero-leak caching.
     *
     * @param itemStack the player head ItemStack
     * @return the resolved face Component, or null
     */
    public Component resolveSkullComponent(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.PLAYER_HEAD) return null;
        checkAndCleanCache();
        try {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta == null) return null;

            PlayerProfile profile = skullMeta.getPlayerProfile();
            if (profile != null) {
                // Check textures property
                for (ProfileProperty prop : profile.getProperties()) {
                    if ("textures".equals(prop.getName())) {
                        String value = prop.getValue();
                        if (value != null && !value.isBlank()) {
                            return skullFaceCache.computeIfAbsent(value, val -> {
                                String json = "{\"object\":\"player\",\"player\":{\"properties\":[{\"name\":\"textures\",\"value\":\"" + val.trim() + "\"}]}}";
                                try {
                                    return GsonComponentSerializer.gson().deserialize(json).append(Component.text(" "));
                                } catch (Exception e) {
                                    return null;
                                }
                            });
                        }
                    }
                }

                // Fallback to profile username
                String name = profile.getName();
                if (name != null && !name.isBlank()) {
                    return skullFaceCache.computeIfAbsent(name, n -> {
                        String json = "{\"object\":\"player\",\"player\":\"" + n + "\"}";
                        try {
                            return GsonComponentSerializer.gson().deserialize(json).append(Component.text(" "));
                        } catch (Exception e) {
                            return null;
                        }
                    });
                }

                // Fallback to profile UUID
                UUID uuid = profile.getId();
                if (uuid != null) {
                    return skullFaceCache.computeIfAbsent(uuid.toString(), u -> {
                        String json = "{\"object\":\"player\",\"player\":{\"id\":\"" + u + "\"}}";
                        try {
                            return GsonComponentSerializer.gson().deserialize(json).append(Component.text(" "));
                        } catch (Exception e) {
                            return null;
                        }
                    });
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Dynamically gets or creates the 2D player face component of the showcasing player.
     *
     * @param player the Player
     * @return the player face Component
     */
    public Component getPlayerFace(Player player) {
        if (player == null) {
            String json = "{\"object\":\"player\",\"player\":\"Steve\"}";
            return GsonComponentSerializer.gson().deserialize(json).append(Component.text(" "));
        }
        return playerFaceCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            String json = "{\"object\":\"player\",\"player\":\"" + player.getName() + "\"}";
            try {
                return GsonComponentSerializer.gson().deserialize(json).append(Component.text(" "));
            } catch (Exception e) {
                return Component.empty();
            }
        });
    }

    @Override
    public void onReload(ChatItem plugin) {
        onLoad(plugin);
    }

    private void checkAndCleanCache() {
        PluginConfig.CacheSettings settings = plugin.getConfigHandler().getConfig().cacheSettings;
        long now = System.currentTimeMillis();
        long expiryMs = settings.skullCacheExpiryHours * 3600000L;

        if (now - lastClearTime > expiryMs || skullFaceCache.size() >= settings.skullCacheMaxSize) {
            skullFaceCache.clear();
            lastClearTime = now;
        }
    }
}
