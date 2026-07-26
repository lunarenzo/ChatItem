package io.github.lunatech.chatitem.utility;


import io.github.lunatech.chatitem.ChatItem;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;

/**
 * A class that provides shorthand access to {@link ChatItem#getComponentLogger}.
 */
public class Logger {
    /**
     * Get component logger. Shorthand for:
     *
     * @return the component logger {@link ChatItem#getComponentLogger}.
     */
    @NotNull
    public static ComponentLogger get() {
        return ChatItem.getInstance().getComponentLogger();
    }
}
