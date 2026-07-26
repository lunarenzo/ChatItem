package io.github.lunatech.chatitem.messaging;

import io.github.lunatech.chatitem.event.MockEvent;
import io.github.lunatech.chatitem.messaging.message.Message;

public class MockSyncMessageEvent extends MockEvent {
    private final Message<?> message;

    public MockSyncMessageEvent(Message<?> message) {
        this.message = message;
    }

    public Message<?> getMessage() {
        return message;
    }
}