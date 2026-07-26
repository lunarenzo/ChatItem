package io.github.lunatech.chatitem.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.lunatech.chatitem.AbstractChatItem;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import org.bukkit.command.CommandSender;

import static io.github.lunatech.chatitem.command.CommandHandler.BASE_PERM;

/**
 * Class containing the code for the chatitem command.
 */
final class ChatItemCommand extends Command {
    private final AbstractChatItem plugin;

    /**
     * Instantiates and registers a new command.
     */
    ChatItemCommand(AbstractChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandAPICommand command() {
        return new CommandAPICommand("chatitem")
            .withHelp("Base command.", "Base command.")
            .withPermission(BASE_PERM)
            .withSubcommands(
                new TranslationCommand().command(),
                new DumpCommand().command()
            )
            .executes(this::executorChatItem);
    }

    private void executorChatItem(CommandSender sender, CommandArguments args) {
        sender.sendMessage(
            ColorParser.of("<white>Read more about CommandAPI &9<click:open_url:'https://commandapi.jorel.dev/9.0.3/'>here</click><white>.")
                .legacy() // Parse legacy color codes
                .build()
        );
    }
}
