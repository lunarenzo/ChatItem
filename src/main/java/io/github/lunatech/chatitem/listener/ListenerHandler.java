package io.github.lunatech.chatitem.listener;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle registration of listeners.
 */
public class ListenerHandler implements Reloadable {
    private final List<Listener> listeners = new ArrayList<>();
    private final ChatItem plugin;

    public ListenerHandler(ChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable(ChatItem plugin) {
        listeners.add(new io.github.lunatech.chatitem.listener.player.AsyncChatListener(plugin));
        listeners.add(new io.github.lunatech.chatitem.listener.player.LegacyChatListener(plugin));
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
    }
}
