package io.github.lunatech.chatitem.hook;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractHook implements Reloadable {
    private final ChatItem plugin;

    protected AbstractHook(ChatItem plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the plugin instance.
     *
     * @return plugin instance
     */
    protected ChatItem getPlugin() {
        return plugin;
    }

    /**
     * Check if this hook is loaded and ready for use.
     *
     * @return boolean whether this hook is loaded or not
     */
    public boolean isHookLoaded() {
        throw new UnsupportedOperationException("Method isHookLoaded() is not implemented");
    }

    /**
     * On plugin load.
     */
    @Override
    public void onLoad(ChatItem plugin) {
    }

    /**
     * On plugin enable.
     */
    @Override
    public void onEnable(ChatItem plugin) {
    }

    /**
     * On plugin disable.
     */
    @Override
    public void onDisable(ChatItem plugin) {
    }

    /**
     * Check if a plugin is present on the server.
     *
     * @param pluginName the plugin name
     * @return boolean whether the plugin is present or not
     */
    public static boolean isPluginPresent(@Nullable String pluginName) {
        return pluginName != null && Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }

    /**
     * Check if a plugin is enabled on the server.
     *
     * @param pluginName the plugin name
     * @return boolean whether the plugin is enabled or not
     */
    public static boolean isPluginEnabled(@Nullable String pluginName) {
        return pluginName != null && Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}
