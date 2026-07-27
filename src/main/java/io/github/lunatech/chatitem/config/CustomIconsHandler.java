package io.github.lunatech.chatitem.config;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.github.lunatech.chatitem.config.loading.ConfigLoader;
import io.github.lunatech.chatitem.utility.Util.SpriteMapping;
import org.bukkit.Material;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomIconsHandler implements Reloadable {
    private final ChatItem plugin;
    private final Path configDir;
    private final Logger logger;

    private CustomIconsConfig cfg;
    private final Map<Material, SpriteMapping> overrideMap = new ConcurrentHashMap<>();

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

        // Compile overrides cache
        overrideMap.clear();
        Map<String, CustomIconsConfig.IconOverride> map = cfg.customIcons;
        if (map != null) {
            for (Map.Entry<String, CustomIconsConfig.IconOverride> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                try {
                    Material material = Material.valueOf(entry.getKey().trim().toUpperCase());
                    String atlas = entry.getValue().atlas;
                    String sprite = entry.getValue().sprite;
                    if (atlas != null && sprite != null) {
                        overrideMap.put(material, new SpriteMapping(atlas, sprite));
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid material ID specified in custom-icons.yml: {}", entry.getKey());
                }
            }
        }
    }

    /**
     * Retrieves the custom sprite override for a material if configured.
     * Checks if the material is excluded first; if it is excluded, return null.
     *
     * @param material the Material to check
     * @return the SpriteMapping override, or null if not overridden or if excluded
     */
    public SpriteMapping getOverride(Material material) {
        if (material == null) return null;

        // Rule: If the item is in the excluded list, it will be ignored and remain hidden.
        if (plugin.getExcludedIconsHandler().isExcluded(material)) {
            return null;
        }

        return overrideMap.get(material);
    }
}
