package io.github.lunatech.chatitem;

/**
 * Implemented in classes that should support being reloaded.
 */
public interface Reloadable {
    /**
     * On plugin load.
     */
    default void onLoad(ChatItem plugin) {
    }

    /**
     * On plugin enable.
     */
    default void onEnable(ChatItem plugin) {
    }

    /**
     * On plugin disable.
     */
    default void onDisable(ChatItem plugin) {
    }

    /**
     * On plugin reload.
     */
    default void onReload(ChatItem plugin) {
    }
}
