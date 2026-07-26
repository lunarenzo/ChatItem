package io.github.lunatech.chatitem.listener;

import io.github.lunatech.chatitem.AbstractChatItem;
import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle registration of event listeners.
 */
public class ListenerHandler implements Reloadable {
    private final ChatItem plugin;
    private final List<Listener> listeners = new ArrayList<>();

    /**
     * Instantiates a the Listener handler.
     *
     * @param plugin the plugin instance
     */
    public ListenerHandler(ChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable(AbstractChatItem plugin) {
        listeners.clear(); // Clear the list to avoid duplicate listeners when reloading the plugin
//        listeners.add(new ChatItemListener());

        // Register listeners here
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}
