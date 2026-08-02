package io.github.lunatech.chatitem.config;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.github.lunatech.chatitem.config.loading.ConfigLoader;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * A class that generates/loads & provides access to a configuration file.
 */
public class ConfigHandler implements Reloadable {
    private final ChatItem plugin;
    private final Path configDir;
    private final Logger logger;

    private PluginConfig cfg;

    /**
     * Instantiates a new Config handler.
     *
     * @param plugin the plugin instance
     */
    public ConfigHandler(ChatItem plugin) {
        this.plugin = plugin;
        this.configDir = plugin.getDataFolder().toPath();
        this.logger = plugin.getComponentLogger();
    }

    public ConfigHandler(ChatItem plugin, Path configDir, Logger logger) {
        this.plugin = plugin;
        this.configDir = configDir;
        this.logger = logger;
    }

    @Override
    public void onLoad(ChatItem plugin) {
        PluginConfig loaded = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("config.yml"))
            .withHeader("")
            .build(PluginConfig.class);

        if (loaded != null) {
            cfg = loaded;
        } else {
            if (cfg == null) {
                cfg = new PluginConfig();
                logger.error("Failed to load configuration at startup. Using default fallback configuration.");
            } else {
                logger.error("Failed to reload configuration. Keeping the previous active configuration in memory.");
            }
        }
    }

    @Override
    public void onReload(ChatItem plugin) {
        onLoad(plugin);
    }

    /**
     * Gets main config object.
     *
     * @return the config object
     */
    public PluginConfig getConfig() {
        return cfg;
    }
}
