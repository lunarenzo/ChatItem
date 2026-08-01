package io.github.lunatech.chatitem.listener.player;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.config.PluginConfig;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.regex.Pattern;

public class AsyncChatListener implements Listener {
    private final ChatItem plugin;

    public AsyncChatListener(ChatItem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component message = event.message();

        // 1. Fast O(1) text check before doing full parser operations
        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
        String plainLower = plainText.toLowerCase();

        boolean hasItemMatch = plainLower.contains("[item]") || plainLower.contains("[i]");
        boolean hasInvMatch = plainLower.contains("[inventory]") || plainLower.contains("[inv]");
        boolean hasEnderMatch = plainLower.contains("[echest]") || plainLower.contains("[ender]") || plainLower.contains("[enderchest]");
        boolean hasShulkerMatch = plainLower.contains("[shulker]");

        if (!hasItemMatch && !hasInvMatch && !hasEnderMatch && !hasShulkerMatch) {
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
                event.setCancelled(true);
                return;
            }
        }

        // 3. Delegate processing
        ShowcaseProcessor.ProcessedResult result = ShowcaseProcessor.processShowcase(plugin, player, message);
        if (result.error) {
            player.sendMessage(result.errorMessage);
            event.setCancelled(true);
            return;
        }
        if (!result.replaced) {
            return;
        }

        event.message(result.replacedMessage);

        final Component finalItemRep = result.itemReplacement;
        final Component finalInvRep = result.invReplacement;
        final Component finalEnderRep = result.enderReplacement;
        final Component finalShulkerRep = result.shulkerReplacement;

        ChatRenderer originalRenderer = event.renderer();
        event.renderer((source, sourceDisplayName, msg, viewer) -> {
            Component rendered = originalRenderer.render(source, sourceDisplayName, msg, viewer);
            Component newRendered = rendered;
            if (finalItemRep != null) {
                newRendered = newRendered.replaceText(TextReplacementConfig.builder()
                    .match(Pattern.compile("(?i)\\[(item|i)\\]"))
                    .replacement(finalItemRep)
                    .build());
            }
            if (finalInvRep != null) {
                newRendered = newRendered.replaceText(TextReplacementConfig.builder()
                    .match(Pattern.compile("(?i)\\[(inventory|inv)\\]"))
                    .replacement(finalInvRep)
                    .build());
            }
            if (finalEnderRep != null) {
                newRendered = newRendered.replaceText(TextReplacementConfig.builder()
                    .match(Pattern.compile("(?i)\\[(echest|ender|enderchest)\\]"))
                    .replacement(finalEnderRep)
                    .build());
            }
            if (finalShulkerRep != null) {
                newRendered = newRendered.replaceText(TextReplacementConfig.builder()
                    .match(Pattern.compile("(?i)\\[shulker\\]"))
                    .replacement(finalShulkerRep)
                    .build());
            }
            return newRendered;
        });

        // 4. Update cooldown
        plugin.getCooldownMap().put(uuid, now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCooldownMap().remove(event.getPlayer().getUniqueId());
        plugin.getLastWarnedMap().remove(event.getPlayer().getUniqueId());
    }
}
