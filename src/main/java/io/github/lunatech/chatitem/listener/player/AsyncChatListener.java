package io.github.lunatech.chatitem.listener.player;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.config.PluginConfig;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

        if (!hasItemMatch && !hasInvMatch) {
            return;
        }

        PluginConfig config = plugin.getConfigHandler().getConfig();
        PluginConfig.ItemShowcase settings = config.itemShowcase;
        PluginConfig.InventoryShowcase invSettings = config.inventoryShowcase;

        boolean hasItemPermission = !settings.permissionRequired || player.hasPermission(settings.permissionNode);
        boolean hasInvPermission = !invSettings.permissionRequired || player.hasPermission(invSettings.permissionNode);

        // If matched but no permission for either, skip
        if ((hasItemMatch && !hasItemPermission) && (hasInvMatch && !hasInvPermission)) {
            return;
        }

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

        // 3. Fetch item in hand and inventory snapshot safely on the Main Tick Thread
        class ShowcaseDetails {
            final ItemStack itemStack;
            final HoverEvent<HoverEvent.ShowItem> itemHover;
            final io.github.lunatech.chatitem.inventory.InventorySnapshot invSnap;

            ShowcaseDetails(ItemStack itemStack, HoverEvent<HoverEvent.ShowItem> itemHover, io.github.lunatech.chatitem.inventory.InventorySnapshot invSnap) {
                this.itemStack = itemStack;
                this.itemHover = itemHover;
                this.invSnap = invSnap;
            }
        }

        CompletableFuture<ShowcaseDetails> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack inHand = null;
            HoverEvent<HoverEvent.ShowItem> inHandHover = null;
            if (hasItemMatch && hasItemPermission) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && !item.getType().isAir()) {
                    inHand = item.clone();
                    inHandHover = inHand.asHoverEvent();
                }
            }

            io.github.lunatech.chatitem.inventory.InventorySnapshot snap = null;
            if (hasInvMatch && hasInvPermission) {
                snap = plugin.getInventoryManager().createSnapshot(player);
            }

            future.complete(new ShowcaseDetails(inHand, inHandHover, snap));
        });

        ShowcaseDetails details;
        try {
            details = future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getComponentLogger().warn("Failed to fetch showcase details for player " + player.getName(), e);
            return;
        }

        // 4. Build replacements
        Component itemReplacement = null;
        if (details.itemStack != null) {
            ItemStack itemStack = details.itemStack;
            HoverEvent<HoverEvent.ShowItem> hoverEvent = details.itemHover;

            ItemMeta meta = itemStack.getItemMeta();
            Component nameComponent;
            if (meta != null && meta.hasDisplayName()) {
                nameComponent = meta.displayName();
            } else {
                String fallbackName = io.github.lunatech.chatitem.utility.Util.getFriendlyMaterialName(itemStack.getType().name());
                nameComponent = Component.translatable(itemStack.getType().translationKey(), fallbackName);
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
                Component iconComponent = plugin.getCustomIconsHandler().getOverride(material, player);
                if (iconComponent == null && material == org.bukkit.Material.PLAYER_HEAD) {
                    iconComponent = plugin.getCustomIconsHandler().resolveSkullComponent(itemStack);
                }
                if (iconComponent == null) {
                    io.github.lunatech.chatitem.utility.Util.SpriteMapping mapping = io.github.lunatech.chatitem.utility.Util.getSpriteMapping(material);
                    String iconTag = settings.iconFormat
                        .replace("{atlas}", mapping.atlas)
                        .replace("{sprite}", mapping.sprite);
                    try {
                        iconComponent = MiniMessage.miniMessage().deserialize(iconTag)
                            .color(net.kyori.adventure.text.format.NamedTextColor.WHITE);
                    } catch (Exception ignored) {}
                }
                if (iconComponent != null) {
                    nameComponent = Component.empty().append(iconComponent).append(nameComponent);
                    rawNameComponent = Component.empty().append(iconComponent).append(rawNameComponent);
                }
            }

            if (hoverEvent != null) {
                nameComponent = nameComponent.hoverEvent(hoverEvent);
                rawNameComponent = rawNameComponent.hoverEvent(hoverEvent);
            }

            Component amountComponent = itemStack.getAmount() > 1
                ? Component.text(" x" + itemStack.getAmount())
                : Component.empty();

            if (settings.itemFormat.isEmpty()) {
                itemReplacement = rawNameComponent.append(amountComponent);
            } else {
                itemReplacement = MiniMessage.miniMessage().deserialize(
                    settings.itemFormat,
                    Placeholder.component("name", nameComponent),
                    Placeholder.component("raw_name", rawNameComponent),
                    Placeholder.component("amount", amountComponent)
                );
            }

            if (hoverEvent != null) {
                itemReplacement = itemReplacement.hoverEvent(hoverEvent);
            }
        }

        Component invReplacement = null;
        if (details.invSnap != null) {
            String token = plugin.getInventoryManager().registerSnapshot(details.invSnap);
            Component playerHead = plugin.getCustomIconsHandler().getPlayerFace(player);
            invReplacement = MiniMessage.miniMessage().deserialize(
                invSettings.inventoryFormat,
                Placeholder.unparsed("player_name", player.getName()),
                Placeholder.component("player_head", playerHead)
            );
            invReplacement = invReplacement
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize(config.messages.invHover)))
                .clickEvent(ClickEvent.runCommand("/chatitem viewinv " + token));
        }

        // 5. Replace text elements in message
        boolean replacedAny = false;
        Component finalMessage = message;

        if (details.itemStack != null) {
            TextReplacementConfig itemConfig = TextReplacementConfig.builder()
                .match(Pattern.compile("(?i)\\[(item|i)\\]"))
                .replacement(itemReplacement)
                .build();
            finalMessage = finalMessage.replaceText(itemConfig);
            replacedAny = true;
        } else if (hasItemMatch && hasItemPermission) {
            Component emptyReplacement = MiniMessage.miniMessage().deserialize(settings.emptyHandFormat);
            TextReplacementConfig itemConfig = TextReplacementConfig.builder()
                .match(Pattern.compile("(?i)\\[(item|i)\\]"))
                .replacement(emptyReplacement)
                .build();
            finalMessage = finalMessage.replaceText(itemConfig);
            replacedAny = true;
        }

        if (invReplacement != null) {
            TextReplacementConfig invConfig = TextReplacementConfig.builder()
                .match(Pattern.compile("(?i)\\[(inventory|inv)\\]"))
                .replacement(invReplacement)
                .build();
            finalMessage = finalMessage.replaceText(invConfig);
            replacedAny = true;
        }

        if (!replacedAny) {
            return;
        }

        event.message(finalMessage);

        final Component finalItemRep = details.itemStack != null ? itemReplacement : (hasItemMatch && hasItemPermission ? MiniMessage.miniMessage().deserialize(settings.emptyHandFormat) : null);
        final Component finalInvRep = invReplacement;

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
            return newRendered;
        });

        // 6. Update cooldown
        plugin.getCooldownMap().put(uuid, now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCooldownMap().remove(event.getPlayer().getUniqueId());
        plugin.getLastWarnedMap().remove(event.getPlayer().getUniqueId());
    }
}
