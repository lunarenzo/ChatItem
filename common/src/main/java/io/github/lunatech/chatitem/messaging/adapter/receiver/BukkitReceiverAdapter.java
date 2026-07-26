package io.github.lunatech.chatitem.messaging.adapter.receiver;

import io.github.lunatech.chatitem.messaging.adapter.receiver.event.MessageReceivedEvent;
import io.github.lunatech.chatitem.messaging.message.Message;
import io.github.milkdrinkers.threadutil.Scheduler;

public class BukkitReceiverAdapter extends ReceiverAdapter {
    @Override
    public void accept(Message<?> message) {
        Scheduler.sync(() -> new MessageReceivedEvent(message).callEvent()).execute();
    }
}
