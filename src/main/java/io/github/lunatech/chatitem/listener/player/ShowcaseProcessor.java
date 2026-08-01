package io.github.lunatech.chatitem.listener.player;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ShowcaseProcessor {

    public static class ProcessedResult {
        public final boolean replaced;
        public final Component replacedMessage;
        public final Component itemReplacement;
        public final Component invReplacement;
        public final Component enderReplacement;

        public ProcessedResult(boolean replaced, Component replacedMessage, Component itemReplacement, Component invReplacement, Component enderReplacement) {
            this.replaced = replaced;
            this.replacedMessage = replacedMessage;
            this.itemReplacement = itemReplacement;
            this.invReplacement = invReplacement;
            this.enderReplacement = enderReplacement;
        }
    }

    /**
     * Scans and processes item, inventory, and ender chest showcases from a message Component.
     * Evaluates permission gates and executes thread-safe main-thread snapshots safely.
     *
     * @param plugin   the ChatItem plugin instance
     * @param player   the player sending the message
     * @param message  the original chat message Component
     * @return the processed result container
     */
    public static ProcessedResult processShowcase(ChatItem plugin, Player player, Component message) {
        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
        String plainLower = plainText.toLowerCase();

        boolean hasItemMatch = plainLower.contains("[item]") || plainLower.contains("[i]");
        boolean hasInvMatch = plainLower.contains("[inventory]") || plainLower.contains("[inv]");
        boolean hasEnderMatch = plainLower.contains("[echest]") || plainLower.contains("[ender]") || plainLower.contains("[enderchest]");

        if (!hasItemMatch && !hasInvMatch && !hasEnderMatch) {
            return new ProcessedResult(false, message, null, null, null);
        }

        PluginConfig config = plugin.getConfigHandler().getConfig();
        PluginConfig.ItemShowcase settings = config.itemShowcase;
        PluginConfig.InventoryShowcase invSettings = config.inventoryShowcase;
        PluginConfig.EnderChestShowcase enderSettings = config.enderChestShowcase;

        boolean hasItemPermission = !settings.permissionRequired || player.hasPermission(settings.permissionNode);
        boolean hasInvPermission = !invSettings.permissionRequired || player.hasPermission(invSettings.permissionNode);
        boolean hasEnderPermission = !enderSettings.permissionRequired || player.hasPermission(enderSettings.permissionNode);

        // If matched but no permission for any, fail fast
        if ((hasItemMatch && !hasItemPermission) && 
            (hasInvMatch && !hasInvPermission) && 
            (hasEnderMatch && !hasEnderPermission)) {
            return new ProcessedResult(false, message, null, null, null);
        }

        // Fetch item in hand, inventory, and ender chest snapshot safely on the Main Tick Thread
        class ShowcaseDetails {
            final ItemStack itemStack;
            final HoverEvent<HoverEvent.ShowItem> itemHover;
            final io.github.lunatech.chatitem.inventory.InventorySnapshot invSnap;
            final io.github.lunatech.chatitem.inventory.EnderChestSnapshot enderSnap;

            ShowcaseDetails(ItemStack itemStack, HoverEvent<HoverEvent.ShowItem> itemHover, 
                            io.github.lunatech.chatitem.inventory.InventorySnapshot invSnap,
                            io.github.lunatech.chatitem.inventory.EnderChestSnapshot enderSnap) {
                this.itemStack = itemStack;
                this.itemHover = itemHover;
                this.invSnap = invSnap;
                this.enderSnap = enderSnap;
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

            io.github.lunatech.chatitem.inventory.EnderChestSnapshot eSnap = null;
            if (hasEnderMatch && hasEnderPermission) {
                eSnap = plugin.getInventoryManager().createEnderChestSnapshot(player);
            }

            future.complete(new ShowcaseDetails(inHand, inHandHover, snap, eSnap));
        });

        ShowcaseDetails details;
        try {
            details = future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getComponentLogger().warn("Failed to fetch showcase details for player " + player.getName(), e);
            return new ProcessedResult(false, message, null, null, null);
        }

        // Build replacements
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

        Component enderReplacement = null;
        if (details.enderSnap != null) {
            String token = plugin.getInventoryManager().registerEnderChestSnapshot(details.enderSnap);
            Component playerHead = plugin.getCustomIconsHandler().getPlayerFace(player);
            enderReplacement = MiniMessage.miniMessage().deserialize(
                enderSettings.enderChestFormat,
                Placeholder.unparsed("player_name", player.getName()),
                Placeholder.component("player_head", playerHead)
            );
            enderReplacement = enderReplacement
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize(config.messages.enderHover)))
                .clickEvent(ClickEvent.runCommand("/chatitem viewender " + token));
        }

        // Replace text elements in message
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
            itemReplacement = emptyReplacement;
        }

        if (invReplacement != null) {
            TextReplacementConfig invConfig = TextReplacementConfig.builder()
                .match(Pattern.compile("(?i)\\[(inventory|inv)\\]"))
                .replacement(invReplacement)
                .build();
            finalMessage = finalMessage.replaceText(invConfig);
            replacedAny = true;
        }

        if (enderReplacement != null) {
            TextReplacementConfig enderConfig = TextReplacementConfig.builder()
                .match(Pattern.compile("(?i)\\[(echest|ender|enderchest)\\]"))
                .replacement(enderReplacement)
                .build();
            finalMessage = finalMessage.replaceText(enderConfig);
            replacedAny = true;
        }

        return new ProcessedResult(replacedAny, finalMessage, itemReplacement, invReplacement, enderReplacement);
    }
}
