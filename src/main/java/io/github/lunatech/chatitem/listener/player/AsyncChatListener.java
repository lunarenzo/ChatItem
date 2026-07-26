package io.github.lunatech.chatitem.listener.player;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.config.PluginConfig;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AsyncChatListener implements Listener {
    private final ChatItem plugin;
    private final Pattern tagPattern = Pattern.compile("(?i)\\[(item|i)\\]");

    public AsyncChatListener(ChatItem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component message = event.message();

        // 1. Fast O(1) text check before doing full parser operations
        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
        if (!plainText.toLowerCase().contains("[item]") && !plainText.toLowerCase().contains("[i]")) {
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
                long remainingSec = (cooldownMs - diff + 999L) / 1000L;
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    settings.cooldownMessage,
                    Placeholder.parsed("cooldown", String.valueOf(remainingSec))
                ));
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
                itemFuture.complete(new ItemDetails(item.clone(), item.asHoverEvent()));
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

            if (settings.showIcon) {
                String materialName = itemStack.getType().name().toLowerCase();
                String atlas = itemStack.getType().isBlock() ? "minecraft:blocks" : "minecraft:items";
                String sprite = (itemStack.getType().isBlock() ? "block/" : "item/") + materialName;
                String iconTag = settings.iconFormat
                    .replace("{atlas}", atlas)
                    .replace("{sprite}", sprite);
                try {
                    Component iconComponent = MiniMessage.miniMessage().deserialize(iconTag)
                        .color(net.kyori.adventure.text.format.NamedTextColor.WHITE);
                    nameComponent = iconComponent.append(nameComponent);
                } catch (Exception e) {
                    // Gracefully ignore if sprite is unsupported
                }
            }

            Component rawNameComponent = nameComponent;
            if (rawNameComponent.color() == null) {
                rawNameComponent = rawNameComponent.color(io.github.lunatech.chatitem.utility.Util.getItemRarityColor(itemStack));
            }

            // Double insurance: attach the hover event directly to the name components
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

            // Double insurance: attach the hover event to the parent wrapper component as well
            if (hoverEvent != null) {
                replacementComponent = replacementComponent.hoverEvent(hoverEvent);
            }
        }

        // 6. Perform the replacement across the message
        TextReplacementConfig replaceConfig = TextReplacementConfig.builder()
            .match(tagPattern)
            .replacement(replacementComponent)
            .build();

        event.message(message.replaceText(replaceConfig));

        // Wrap the chat renderer to ensure compatibility with formatting plugins that serialize/deserialize components (like EssentialsChat)
        ChatRenderer originalRenderer = event.renderer();
        event.renderer((source, sourceDisplayName, msg, viewer) -> {
            Component rendered = originalRenderer.render(source, sourceDisplayName, msg, viewer);
            return rendered.replaceText(replaceConfig);
        });

        // 7. Update cooldown
        plugin.getCooldownMap().put(uuid, now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Prevent memory accumulation by cleaning entries when players disconnect
        plugin.getCooldownMap().remove(event.getPlayer().getUniqueId());
    }
}
