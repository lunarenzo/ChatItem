package io.github.lunatech.chatitem.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * The ChatItemAPI class is the main entry point for accessing the ChatItem API.
 */
public abstract class ChatItemAPI {
    private static ChatItemAPI INSTANCE;

    /**
     * Gets the instance of the ChatItemAPI.
     *
     * @return the instance of ChatItemAPI
     * @since 1.0.0
     */
    public static ChatItemAPI getInstance() {
        if (INSTANCE == null)
            throw new RuntimeException("API was accessed before being initialized!");
        return INSTANCE;
    }

    /**
     * Sets the instance of the ChatItemAPI.
     * This method is intended for internal use by the api provider only.
     *
     * @param api the instance of ChatItemAPI to set
     * @since 1.0.0
     */
    @ApiStatus.Internal
    protected static void setInstance(ChatItemAPI api) {
        INSTANCE = api;
    }
}
