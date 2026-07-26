package io.github.lunatech.chatitem.event;

@FunctionalInterface
public interface MockEventListener {
    void onEvent(MockEvent event);
}