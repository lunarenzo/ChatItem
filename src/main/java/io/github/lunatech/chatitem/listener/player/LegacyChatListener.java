package io.github.lunatech.chatitem.listener.player;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.HoverEvent;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class LegacyChatListener implements Listener {
    private final ChatItem plugin;
    private final Pattern tagPattern = Pattern.compile("(?i)\\[(item|i)\\]");

    public LegacyChatListener(ChatItem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 1. Fast text check
        if (!message.toLowerCase().contains("[item]") && !message.toLowerCase().contains("[i]")) {
            return;
        }

        PluginConfig config = plugin.getConfigHandler().getConfig();
        PluginConfig.ItemShowcase settings = config.itemShowcase;

        // 2. Permission Check
        if (settings.permissionRequired && !player.hasPermission(settings.permissionNode)) {
            return;
        }

        // 3. Cooldown Check
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

        // 4. Fetch item in hand safely on the Main Tick Thread
        class ItemDetails {
            final ItemStack stack;
            final HoverEvent<HoverEvent.ShowItem> hover;

            ItemDetails(ItemStack stack, HoverEvent<HoverEvent.ShowItem> hover) {
                this.stack = stack;
                this.hover = hover;
            }
        }
        CompletableFuture<ItemDetails> itemFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                itemFuture.complete(new ItemDetails(null, null));
            } else {
                ItemStack clone = item.clone();
                itemFuture.complete(new ItemDetails(clone, clone.asHoverEvent()));
            }
        });

        ItemDetails details;
        try {
            details = itemFuture.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getComponentLogger().warn("Failed to fetch item in main hand for player " + player.getName(), e);
            return;
        }

        ItemStack itemStack = details.stack;
        HoverEvent<HoverEvent.ShowItem> hoverEvent = details.hover;

        // 5. Build the replacement Component
        Component replacementComponent;
        if (itemStack == null || itemStack.getType().isAir()) {
            replacementComponent = MiniMessage.miniMessage().deserialize(settings.emptyHandFormat);
        } else {
            ItemMeta meta = itemStack.getItemMeta();
            Component nameComponent;
            if (meta != null && meta.hasDisplayName()) {
                nameComponent = meta.displayName();
            } else {
                String fallbackName = io.github.lunatech.chatitem.utility.Util.getFriendlyMaterialName(itemStack.getType().name());
                nameComponent = Component.translatable(itemStack.getType().translationKey(), fallbackName);
            }

            if (itemStack.getAmount() > 1) {
                nameComponent = nameComponent.append(Component.text(" x" + itemStack.getAmount()));
            }

            Component rawNameComponent = nameComponent;
            net.kyori.adventure.text.format.TextColor rarityColor = itemStack.displayName().color();
            if (rarityColor != null) {
                rawNameComponent = rawNameComponent.color(rarityColor);
            } else {
                rawNameComponent = rawNameComponent.color(net.kyori.adventure.text.format.NamedTextColor.WHITE);
            }

            if (settings.showIcon && !plugin.getExcludedIconsHandler().isExcluded(itemStack.getType())) {
                org.bukkit.Material material = itemStack.getType();
                if (material == org.bukkit.Material.ENCHANTED_GOLDEN_APPLE) {
                    material = org.bukkit.Material.GOLDEN_APPLE;
                }
                io.github.lunatech.chatitem.utility.Util.SpriteMapping mapping = plugin.getCustomIconsHandler().getOverride(material);
                if (mapping == null) {
                    mapping = io.github.lunatech.chatitem.utility.Util.getSpriteMapping(material);
                }
                String iconTag = settings.iconFormat
                    .replace("{atlas}", mapping.atlas)
                    .replace("{sprite}", mapping.sprite);
                try {
                    Component iconComponent = MiniMessage.miniMessage().deserialize(iconTag)
                        .color(net.kyori.adventure.text.format.NamedTextColor.WHITE);
                    nameComponent = iconComponent.append(nameComponent);
                    rawNameComponent = iconComponent.append(rawNameComponent);
                } catch (Exception e) {
                    // Gracefully ignore if sprite is unsupported
                }
            }

            if (hoverEvent != null) {
                nameComponent = nameComponent.hoverEvent(hoverEvent);
                rawNameComponent = rawNameComponent.hoverEvent(hoverEvent);
            }

            if (settings.itemFormat.isEmpty()) {
                replacementComponent = rawNameComponent;
            } else {
                replacementComponent = MiniMessage.miniMessage().deserialize(
                    settings.itemFormat,
                    Placeholder.component("name", nameComponent),
                    Placeholder.component("raw_name", rawNameComponent)
                );
            }

            if (hoverEvent != null) {
                replacementComponent = replacementComponent.hoverEvent(hoverEvent);
            }
        }

        // 6. Perform the replacement across the message Component
        TextReplacementConfig replaceConfig = TextReplacementConfig.builder()
            .match(tagPattern)
            .replacement(replacementComponent)
            .build();

        Component displayNameComponent = LegacyComponentSerializer.legacySection().deserialize(player.getDisplayName());
        Component messageComponent = LegacyComponentSerializer.legacySection().deserialize(message).replaceText(replaceConfig);

        // 7. Format the final output Component using Spigot format structure
        Component finalComponent = formatLegacyChat(event.getFormat(), displayNameComponent, messageComponent);

        // 8. Cancel event and manually broadcast Component to all recipients
        event.setCancelled(true);
        for (Player recipient : event.getRecipients()) {
            recipient.sendMessage(finalComponent);
        }
        Bukkit.getConsoleSender().sendMessage(finalComponent);

        // 9. Update cooldown
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
