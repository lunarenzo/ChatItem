package io.github.lunatech.chatitem;

import io.github.lunatech.chatitem.config.ConfigHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractChatItem extends JavaPlugin {
    private static AbstractChatItem instance;

    /**
     * Gets plugin instance.
     *
     * @return the plugin instance
     */
    public static AbstractChatItem getInstance() {
        return AbstractChatItem.instance;
    }

    AbstractChatItem() {
        AbstractChatItem.instance = this;
    }

    /**
     * Gets config handler.
     *
     * @return the config handler
     */
    public abstract @NotNull ConfigHandler getConfigHandler();
}
