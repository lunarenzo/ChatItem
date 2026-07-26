package io.github.lunatech.chatitem.hook.placeholderapi;

import io.github.lunatech.chatitem.AbstractChatItem;
import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.hook.AbstractHook;
import io.github.lunatech.chatitem.hook.Hook;

/**
 * A hook to interface with <a href="https://wiki.placeholderapi.com/">PlaceholderAPI</a>.
 */
public class PAPIHook extends AbstractHook {
    private PAPIExpansion PAPIExpansion;

    /**
     * Instantiates a new PlaceholderAPI hook.
     *
     * @param plugin the plugin instance
     */
    public PAPIHook(ChatItem plugin) {
        super(plugin);
    }

    @Override
    public void onEnable(AbstractChatItem plugin) {
        if (!isHookLoaded())
            return;

        PAPIExpansion = new PAPIExpansion(super.getPlugin());
        PAPIExpansion.register();
    }

    @Override
    public void onDisable(AbstractChatItem plugin) {
        if (!isHookLoaded())
            return;

        PAPIExpansion.unregister();
        PAPIExpansion = null;
    }

    @Override
    public boolean isHookLoaded() {
        return isPluginPresent(Hook.PAPI.getPluginName()) && isPluginEnabled(Hook.PAPI.getPluginName());
    }
}
