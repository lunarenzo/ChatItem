package io.github.lunatech.chatitem.command;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommandHandler implements Reloadable, TabExecutor {
    private final ChatItem plugin;

    public CommandHandler(ChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable(ChatItem plugin) {
        PluginCommand command = plugin.getCommand("chatitem");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("ChatItem version " + plugin.getPluginMeta().getVersion(), NamedTextColor.GOLD));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            if (!sender.hasPermission("chatitem.command.reload")) {
                sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            plugin.onReload();
            sender.sendMessage(Component.text("ChatItem configuration reloaded successfully.", NamedTextColor.GREEN));
            return true;
        }

        if ("debugicon".equals(sub)) {
            if (!sender.hasPermission("chatitem.command.debugicon")) {
                sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /chatitem debugicon <material>", NamedTextColor.RED));
                return true;
            }
            String matName = args[1].toUpperCase(Locale.ROOT);
            Material material;
            try {
                material = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text("Invalid material: " + matName, NamedTextColor.RED));
                return true;
            }

            Player player = sender instanceof Player ? (Player) sender : null;
            Component icon = plugin.getCustomIconsHandler().getOverride(material, player);
            if (icon == null) {
                io.github.lunatech.chatitem.utility.Util.SpriteMapping mapping = io.github.lunatech.chatitem.utility.Util.getSpriteMapping(material);
                String iconFormat = plugin.getConfigHandler().getConfig().itemShowcase.iconFormat;
                String iconTag = iconFormat
                    .replace("{atlas}", mapping.atlas)
                    .replace("{sprite}", mapping.sprite);
                try {
                    icon = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(iconTag)
                        .color(NamedTextColor.WHITE);
                } catch (Exception ignored) {}
            }

            if (icon != null) {
                sender.sendMessage(Component.text("Icon for " + material.name() + ": ", NamedTextColor.GREEN).append(icon));
            } else {
                sender.sendMessage(Component.text("No icon could be resolved for: " + material.name(), NamedTextColor.RED));
            }
            return true;
        }

        sender.sendMessage(Component.text("Unknown subcommand. Use reload or debugicon.", NamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("chatitem.command.reload")) completions.add("reload");
            if (sender.hasPermission("chatitem.command.debugicon")) completions.add("debugicon");
            return filterPrefix(completions, args[0]);
        }
        if (args.length == 2 && "debugicon".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("chatitem.command.debugicon")) return Collections.emptyList();
            List<String> materials = new ArrayList<>();
            for (Material material : Material.values()) {
                materials.add(material.name().toLowerCase(Locale.ROOT));
            }
            return filterPrefix(materials, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return list.stream()
            .filter(s -> s.startsWith(lower))
            .limit(20)
            .toList();
    }
}