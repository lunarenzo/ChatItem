package io.github.lunatech.chatitem.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;

import java.util.List;

public class CommandHandler implements Reloadable {
    private final ChatItem plugin;

    public CommandHandler(ChatItem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable(ChatItem plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            // Build the main chatitem command structure using Brigadier
            LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("chatitem")
                .requires(source -> source.getSender().hasPermission("chatitem.command"))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Component.text("ChatItem version " + plugin.getPluginMeta().getVersion(), NamedTextColor.GOLD));
                    return Command.SINGLE_SUCCESS;
                });

            // Subcommand: reload (reloads configurations and flushes face / metadata caches)
            builder.then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("chatitem.command.reload"))
                .executes(ctx -> {
                    plugin.onReload();
                    ctx.getSource().getSender().sendMessage(Component.text("ChatItem configuration reloaded successfully.", NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
            );

            // Subcommand: debugicon <material> (allows diagnostic visualization of configured overrides)
            builder.then(Commands.literal("debugicon")
                .requires(source -> source.getSender().hasPermission("chatitem.command.debugicon"))
                .then(Commands.argument("material", ArgumentTypes.resource(RegistryKey.ITEM))
                    .executes(ctx -> {
                        ItemType itemType = ctx.getArgument("material", ItemType.class);
                        Material material = Material.matchMaterial(itemType.getKey().toString());
                        if (material == null) {
                            ctx.getSource().getSender().sendMessage(Component.text("Invalid material: " + itemType.getKey().toString(), NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }

                        CommandSender sender = ctx.getSource().getSender();
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
                        return Command.SINGLE_SUCCESS;
                    })
                )
            );

            LiteralCommandNode<CommandSourceStack> node = builder.build();
            commands.registrar().register(node, "Main command for ChatItem", List.of("ci"));
        });
    }
}