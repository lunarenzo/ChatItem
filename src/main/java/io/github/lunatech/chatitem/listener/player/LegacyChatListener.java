package io.github.lunatech.chatitem.listener.player;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class LegacyChatListener implements Listener {
    private final ChatItem plugin;

    public LegacyChatListener(ChatItem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 1. Fast text check
        String plainLower = message.toLowerCase();
        boolean hasItemMatch = plainLower.contains("[item]") || plainLower.contains("[i]");
        boolean hasInvMatch = plainLower.contains("[inventory]") || plainLower.contains("[inv]");
        boolean hasEnderMatch = plainLower.contains("[echest]") || plainLower.contains("[ender]") || plainLower.contains("[enderchest]");

        if (!hasItemMatch && !hasInvMatch && !hasEnderMatch) {
            return;
        }

        PluginConfig config = plugin.getConfigHandler().getConfig();
        PluginConfig.ItemShowcase settings = config.itemShowcase;

        // 2. Cooldown Check
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (plugin.getCooldownMap().containsKey(uuid)) {
            long lastUsed = plugin.getCooldownMap().get(uuid);
            long diff = now - lastUsed;
            long cooldownMs = settings.cooldownSeconds * 1000L;
            if (diff < cooldownMs) {
                long lastWarned = plugin.getLastWarnedMap().getOrDefault(uuid, 0L);
                if (now - lastWarned >= 500L) {
                    plugin.getLastWarnedMap().put(uuid, now);
                    long remainingSec = (cooldownMs - diff + 999L) / 1000L;
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                        settings.cooldownMessage,
                        Placeholder.parsed("cooldown", String.valueOf(remainingSec))
                    ));
                }
                return;
            }
        }

        // 3. Delegate processing
        Component messageComponent = LegacyComponentSerializer.legacySection().deserialize(message);
        ShowcaseProcessor.ProcessedResult result = ShowcaseProcessor.processShowcase(plugin, player, messageComponent);
        if (!result.replaced) {
            return;
        }

        // 4. Format the final output Component using Spigot format structure
        Component displayNameComponent = LegacyComponentSerializer.legacySection().deserialize(player.getDisplayName());
        Component finalComponent = formatLegacyChat(event.getFormat(), displayNameComponent, result.replacedMessage);

        // 5. Cancel event and manually broadcast Component to all recipients
        event.setCancelled(true);
        for (Player recipient : event.getRecipients()) {
            recipient.sendMessage(finalComponent);
        }
        Bukkit.getConsoleSender().sendMessage(finalComponent);

        // 6. Update cooldown
        plugin.getCooldownMap().put(uuid, now);
    }

    private Component formatLegacyChat(String format, Component displayName, Component message) {
        int firstPlaceholder = format.indexOf("%1$s");
        int secondPlaceholder = format.indexOf("%2$s");

        if (firstPlaceholder == -1 || secondPlaceholder == -1) {
            return Component.text()
                .append(displayName)
                .append(Component.text(": "))
                .append(message)
                .build();
        }

        Component result = Component.empty();
        if (firstPlaceholder < secondPlaceholder) {
            String part1 = format.substring(0, firstPlaceholder);
            String part2 = format.substring(firstPlaceholder + 4, secondPlaceholder);
            String part3 = format.substring(secondPlaceholder + 4);

            return result
                .append(LegacyComponentSerializer.legacySection().deserialize(part1))
                .append(displayName)
                .append(LegacyComponentSerializer.legacySection().deserialize(part2))
                .append(message)
                .append(LegacyComponentSerializer.legacySection().deserialize(part3));
        } else {
            String part1 = format.substring(0, secondPlaceholder);
            String part2 = format.substring(secondPlaceholder + 4, firstPlaceholder);
            String part3 = format.substring(firstPlaceholder + 4);

            return result
                .append(LegacyComponentSerializer.legacySection().deserialize(part1))
                .append(message)
                .append(LegacyComponentSerializer.legacySection().deserialize(part2))
                .append(displayName)
                .append(LegacyComponentSerializer.legacySection().deserialize(part3));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCooldownMap().remove(event.getPlayer().getUniqueId());
        plugin.getLastWarnedMap().remove(event.getPlayer().getUniqueId());
    }
}
