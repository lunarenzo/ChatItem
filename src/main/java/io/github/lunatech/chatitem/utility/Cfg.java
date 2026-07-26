package io.github.lunatech.chatitem.utility;

import io.github.lunatech.chatitem.AbstractChatItem;
import io.github.lunatech.chatitem.config.ConfigHandler;
import io.github.lunatech.chatitem.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Convenience class for accessing {@link ConfigHandler#getConfig}
 */
public final class Cfg {
    /**
     * Convenience method for {@link ConfigHandler#getConfig} to getConnection {@link PluginConfig}
     *
     * @return the config
     */
    @NotNull
    public static PluginConfig get() {
        return AbstractChatItem.getInstance().getConfigHandler().getConfig();
    }
}
