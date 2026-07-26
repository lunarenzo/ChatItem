package io.github.lunatech.chatitem.command;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;

/**
 * A class to handle registration of commands.
 */
public class CommandHandler implements Reloadable {
    private final ChatItem plugin;

    /**
     * Instantiates the Command handler.
     *
     * @param plugin the plugin
     */
    public CommandHandler(ChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(ChatItem plugin) {
    }

    @Override
    public void onEnable(ChatItem plugin) {
        // Native Brigadier commands will be registered in the next phase
    }

    @Override
    public void onDisable(ChatItem plugin) {
    }
}