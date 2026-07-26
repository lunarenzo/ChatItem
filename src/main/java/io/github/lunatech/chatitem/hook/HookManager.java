package io.github.lunatech.chatitem.hook;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.github.lunatech.chatitem.utility.Logger;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.HashMap;

/**
 * Manages lifecycle of all hook objects.
 */
public class HookManager implements Reloadable {
    private final HashMap<Class<? extends AbstractHook>, AbstractHook> hooks = new HashMap<>();
    private final ChatItem plugin;

    public HookManager(ChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(ChatItem plugin) {
        for (Hook hook : Hook.values()) {
            try {
                if (hook.getPluginName() != null && Bukkit.getPluginManager().getPlugin(hook.getPluginName()) == null) {
                    Logger.get().warn(
                        ColorParser.of("<yellow><plugin> is not installed on this server. <plugin> support has been disabled.")
                            .with("plugin", hook.getPluginName())
                            .build()
                    );

                    if (!hook.isOptional())
                        Bukkit.getPluginManager().disablePlugin(plugin);
                    continue;
                }

                final AbstractHook hookInstance = hook.getHookClass().getDeclaredConstructor(ChatItem.class).newInstance(plugin);
                getHooks().put(hook.getHookClass(), hookInstance);
                hook.setHook(getHooks().get(hook.getHookClass()));

                hookInstance.onLoad(plugin);

                if (hook.getPluginName() != null) {
                    Logger.get().info(
                        ColorParser.of("<green><plugin> has been found on this server. <plugin> support enabled.")
                            .with("plugin", hook.getPluginName())
                            .build()
                    );
                }
            } catch (Exception e) {
                Logger.get().warn(
                    ColorParser.of("<yellow><hook> failed to load: <exception>")
                        .with("hook", hook.getHookClass().getName())
                        .with("exception", e.getMessage())
                        .build()
                );
            }
        }
    }

    @Override
    public void onEnable(ChatItem plugin) {
        for (AbstractHook hook : getHooks().values()) {
            hook.onEnable(plugin);

            if (hook instanceof Listener listener)
                Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
    }

    @Override
    public void onDisable(ChatItem plugin) {
        for (AbstractHook hook : getHooks().values()) {
            hook.onDisable(plugin);
        }
        Hook.clearHooks();
        getHooks().clear();
    }

    public HashMap<Class<? extends AbstractHook>, AbstractHook> getHooks() {
        return hooks;
    }
}
