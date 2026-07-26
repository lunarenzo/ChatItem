package io.github.lunatech.chatitem.cooldown.listener;

import io.github.lunatech.chatitem.AbstractChatItem;
import io.github.lunatech.chatitem.Reloadable;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle registration of event listeners.
 */
@SuppressWarnings("FieldCanBeLocal")
public class ListenerHandler implements Reloadable {
    private final AbstractChatItem plugin;
    private final List<Listener> listeners = new ArrayList<>();

    public ListenerHandler(AbstractChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(AbstractChatItem plugin) {
    }

    @Override
    public void onEnable(AbstractChatItem plugin) {
        listeners.clear();
        listeners.add(new CooldownListener(plugin));

        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    @Override
    public void onDisable(AbstractChatItem plugin) {
    }
}
