package io.github.lunatech.chatitem;

/**
 * Implemented in classes that should support being reloaded IE executing the methods during runtime after startup.
 */
public interface Reloadable {
    /**
     * On plugin load.
     */
    default void onLoad(AbstractChatItem plugin) {
    }

    /**
     * On plugin enable.
     */
    default void onEnable(AbstractChatItem plugin) {
    }

    /**
     * On plugin disable.
     */
    default void onDisable(AbstractChatItem plugin) {
    }

}
