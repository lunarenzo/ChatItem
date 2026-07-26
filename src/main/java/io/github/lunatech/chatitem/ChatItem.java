package io.github.lunatech.chatitem;

import io.github.lunatech.chatitem.command.CommandHandler;
import io.github.lunatech.chatitem.config.ConfigHandler;
import io.github.lunatech.chatitem.hook.HookManager;
import io.github.lunatech.chatitem.listener.ListenerHandler;
import io.github.lunatech.chatitem.threadutil.SchedulerHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChatItem extends JavaPlugin {
    private static ChatItem instance;

    private ConfigHandler configHandler;
    private HookManager hookManager;
    private CommandHandler commandHandler;
    private ListenerHandler listenerHandler;
    private SchedulerHandler schedulerHandler;

    private List<? extends Reloadable> handlers;

    public static ChatItem getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;

        configHandler = new ConfigHandler(this);
        hookManager = new HookManager(this);
        commandHandler = new CommandHandler(this);
        listenerHandler = new ListenerHandler(this);
        schedulerHandler = new SchedulerHandler();

        handlers = List.of(
            configHandler,
            hookManager,
            commandHandler,
            listenerHandler,
            schedulerHandler
        );

        for (Reloadable handler : handlers) {
            handler.onLoad(this);
        }
    }

    @Override
    public void onEnable() {
        for (Reloadable handler : handlers) {
            handler.onEnable(this);
        }
    }

    @Override
    public void onDisable() {
        for (Reloadable handler : handlers.reversed()) {
            handler.onDisable(this);
        }
    }

    public void onReload() {
        onDisable();
        onLoad();
        onEnable();
    }

    public @NotNull ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public @NotNull HookManager getHookManager() {
        return hookManager;
    }

    private final java.util.Map<java.util.UUID, Long> cooldownMap = new java.util.concurrent.ConcurrentHashMap<>();

    public @NotNull java.util.Map<java.util.UUID, Long> getCooldownMap() {
        return cooldownMap;
    }
}
