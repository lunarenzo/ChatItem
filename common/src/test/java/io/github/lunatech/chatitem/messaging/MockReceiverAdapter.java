package io.github.lunatech.chatitem.messaging;

import io.github.lunatech.chatitem.event.MockEventSystem;
import io.github.lunatech.chatitem.messaging.adapter.receiver.ReceiverAdapter;
import io.github.lunatech.chatitem.messaging.message.Message;

public class MockReceiverAdapter extends ReceiverAdapter {
    @Override
    public void accept(Message<?> message) {
        MockEventSystem.fireEvent(new MockSyncMessageEvent(message));
    }
}
