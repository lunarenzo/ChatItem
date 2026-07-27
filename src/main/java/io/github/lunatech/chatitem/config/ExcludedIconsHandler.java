package io.github.lunatech.chatitem.config;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.github.lunatech.chatitem.config.loading.ConfigLoader;
import org.bukkit.Material;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExcludedIconsHandler implements Reloadable {
    private final ChatItem plugin;
    private final Path configDir;
    private final Logger logger;

    private ExcludedIconsConfig cfg;
    private final Set<String> exactExclusions = new HashSet<>();
    private final Set<String> wildcardExclusions = new HashSet<>();

    public ExcludedIconsHandler(ChatItem plugin) {
        this.plugin = plugin;
        this.configDir = plugin.getDataFolder().toPath();
        this.logger = plugin.getComponentLogger();
    }

    @Override
    public void onLoad(ChatItem plugin) {
        cfg = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("excluded-icons.yml"))
            .withHeader("")
            .build(ExcludedIconsConfig.class);

        // Compile exclusions cache
        exactExclusions.clear();
        wildcardExclusions.clear();
        List<String> list = cfg.excludedIcons;
        if (list != null) {
            for (String item : list) {
                if (item == null || item.isBlank()) continue;
                String normalized = item.trim().toUpperCase();
                if (normalized.startsWith("*")) {
                    wildcardExclusions.add(normalized.substring(1));
                } else {
                    exactExclusions.add(normalized);
                }
            }
        }
    }

    /**
     * Checks if a material's icon is excluded.
     *
     * @param material the Material to check
     * @return true if the icon should be hidden
     */
    public boolean isExcluded(Material material) {
        if (material == null) return true;
        String name = material.name();
        if (exactExclusions.contains(name)) {
            return true;
        }
        for (String suffix : wildcardExclusions) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
