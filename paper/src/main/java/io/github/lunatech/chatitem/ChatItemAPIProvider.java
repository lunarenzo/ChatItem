package io.github.lunatech.chatitem;

import io.github.lunatech.chatitem.api.ChatItemAPI;

class ChatItemAPIProvider extends ChatItemAPI implements Reloadable {
    private final ChatItem plugin;

    ChatItemAPIProvider(ChatItem plugin) {
        super();
        this.plugin = plugin;
        setInstance(this);
    }
}
