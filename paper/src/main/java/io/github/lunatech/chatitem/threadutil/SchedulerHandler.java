package io.github.lunatech.chatitem.threadutil;

import io.github.lunatech.chatitem.AbstractChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.github.milkdrinkers.threadutil.PlatformBukkit;
import io.github.milkdrinkers.threadutil.Scheduler;

import java.time.Duration;

/**
 * A wrapper handler class for handling thread-util lifecycle.
 */
public class SchedulerHandler implements Reloadable {
    @Override
    public void onLoad(AbstractChatItem plugin) {
        Scheduler.init(new PlatformBukkit(plugin)); // Initialize thread-util
        Scheduler.setErrorHandler(e -> plugin.getSLF4JLogger().error("[Scheduler]: {}", e.getMessage()));
    }

    @Override
    public void onDisable(AbstractChatItem plugin) {
        if (Scheduler.isInitialized())
            Scheduler.shutdown(Duration.ofSeconds(60));
    }
}
