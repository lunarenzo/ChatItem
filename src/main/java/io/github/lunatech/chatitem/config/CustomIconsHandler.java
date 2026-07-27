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
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomIconsHandler implements Reloadable {
    private final ChatItem plugin;
    private final Path configDir;
    private final Logger logger;

    private CustomIconsConfig cfg;
    private final Map<Material, CustomIconsConfig.IconOverride> overrideMap = new ConcurrentHashMap<>();
    private final Map<Material, Component> staticCache = new ConcurrentHashMap<>();

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

        Map<String, CustomIconsConfig.IconOverride> map = cfg.customIcons;
        if (map != null) {
            for (Map.Entry<String, CustomIconsConfig.IconOverride> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                try {
                    Material material = Material.valueOf(entry.getKey().trim().toUpperCase());
                    CustomIconsConfig.IconOverride override = entry.getValue();
                    overrideMap.put(material, override);

                    // Compile static cache if it doesn't depend on dynamic {player} placeholder
                    if (isStatic(override)) {
                        Component component = buildComponent(override, null);
                        if (component != null) {
                            staticCache.put(material, component);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid material ID specified in custom-icons.yml: {}", entry.getKey());
                }
            }
        }
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
            return buildComponent(override, player != null ? player.getName() : null);
        }

        return null;
    }
}
