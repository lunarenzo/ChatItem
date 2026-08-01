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
    private io.github.lunatech.chatitem.config.ExcludedIconsHandler excludedIconsHandler;
    private io.github.lunatech.chatitem.config.CustomIconsHandler customIconsHandler;
    private HookManager hookManager;
    private CommandHandler commandHandler;
    private ListenerHandler listenerHandler;
    private SchedulerHandler schedulerHandler;
    private io.github.lunatech.chatitem.inventory.InventoryManager inventoryManager;

    private List<? extends Reloadable> handlers;

    public static ChatItem getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;

        configHandler = new ConfigHandler(this);
        excludedIconsHandler = new io.github.lunatech.chatitem.config.ExcludedIconsHandler(this);
        customIconsHandler = new io.github.lunatech.chatitem.config.CustomIconsHandler(this);
        hookManager = new HookManager(this);
        commandHandler = new CommandHandler(this);
        listenerHandler = new ListenerHandler(this);
        schedulerHandler = new SchedulerHandler();
        inventoryManager = new io.github.lunatech.chatitem.inventory.InventoryManager(this);

        handlers = List.of(
            configHandler,
            excludedIconsHandler,
            customIconsHandler,
            hookManager,
            commandHandler,
            listenerHandler,
            schedulerHandler,
            inventoryManager
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
        getServer().getPluginManager().registerEvents(new io.github.lunatech.chatitem.inventory.InventoryListener(this), this);
    }

    @Override
    public void onDisable() {
        for (Reloadable handler : handlers.reversed()) {
            handler.onDisable(this);
        }
    }

    public void onReload() {
        for (Reloadable handler : handlers) {
            handler.onReload(this);
        }
    }

    public @NotNull ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public @NotNull io.github.lunatech.chatitem.config.ExcludedIconsHandler getExcludedIconsHandler() {
        return excludedIconsHandler;
    }

    public @NotNull io.github.lunatech.chatitem.config.CustomIconsHandler getCustomIconsHandler() {
        return customIconsHandler;
    }

    public @NotNull HookManager getHookManager() {
        return hookManager;
    }

    private final java.util.Map<java.util.UUID, Long> cooldownMap = new java.util.concurrent.ConcurrentHashMap<>();

    public @NotNull java.util.Map<java.util.UUID, Long> getCooldownMap() {
        return cooldownMap;
    }

    private final java.util.Map<java.util.UUID, Long> lastWarnedMap = new java.util.concurrent.ConcurrentHashMap<>();

    public @NotNull java.util.Map<java.util.UUID, Long> getLastWarnedMap() {
        return lastWarnedMap;
    }

    public @NotNull io.github.lunatech.chatitem.inventory.InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}
