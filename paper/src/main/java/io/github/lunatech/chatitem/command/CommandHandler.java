package io.github.lunatech.chatitem.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import io.github.lunatech.chatitem.AbstractChatItem;
import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;

/**
 * A class to handle registration of commands.
 */
public class CommandHandler implements Reloadable {
    public static final String BASE_PERM = "chatitem.command";
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
    public void onLoad(AbstractChatItem plugin) {
        CommandAPI.onLoad(
            new CommandAPIPaperConfig(plugin)
                .silentLogs(true)
        );
    }

    @Override
    public void onEnable(AbstractChatItem plugin) {
        if (!CommandAPI.isLoaded())
            return;

        CommandAPI.onEnable();

        // Register commands here
        new ChatItemCommand(plugin)
            .command()
            .withAliases()
            .register();
    }

    @Override
    public void onDisable(AbstractChatItem plugin) {
        if (!CommandAPI.isLoaded())
            return;

        CommandAPI.onDisable();
    }
}