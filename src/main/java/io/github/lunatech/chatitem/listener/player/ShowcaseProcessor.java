package io.github.lunatech.chatitem.listener.player;

import io.github.lunatech.chatitem.ChatItem;
import io.github.lunatech.chatitem.config.PluginConfig;
import io.github.lunatech.chatitem.inventory.EnderChestSnapshot;
import io.github.lunatech.chatitem.inventory.InventorySnapshot;
import io.github.lunatech.chatitem.inventory.ShulkerSnapshot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ShowcaseProcessor {

    public static class ReplacementInstruction {
        public final Pattern pattern;
        public final Component replacement;

        public ReplacementInstruction(Pattern pattern, Component replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }
    }

    public static class ProcessedResult {
        public final boolean replaced;
        public final Component replacedMessage;
        public final List<ReplacementInstruction> replacements;
        public final boolean error;
        public final Component errorMessage;

        public ProcessedResult(boolean replaced, Component replacedMessage, List<ReplacementInstruction> replacements) {
            this(replaced, replacedMessage, replacements, false, null);
        }

        public ProcessedResult(boolean replaced, Component replacedMessage, List<ReplacementInstruction> replacements,
                               boolean error, Component errorMessage) {
            this.replaced = replaced;
            this.replacedMessage = replacedMessage;
            this.replacements = replacements;
            this.error = error;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Fast check to see if a plain text message contains any built-in or custom showcase tags.
     *
     * @param plugin the plugin instance
     * @param plainText the plain text message
     * @return true if there is a match
     */
    public static boolean hasShowcaseTag(ChatItem plugin, String plainText) {
        String lower = plainText.toLowerCase();
        if (lower.contains("[item]") || lower.contains("[i]") ||
            lower.contains("[inventory]") || lower.contains("[inv]") ||
            lower.contains("[echest]") || lower.contains("[ender]") || lower.contains("[enderchest]") ||
            lower.contains("[shulker]")) {
            return true;
        }
        PluginConfig config = plugin.getConfigHandler().getConfig();
        if (config.customPlaceholders != null) {
            for (PluginConfig.CustomPlaceholder cp : config.customPlaceholders.values()) {
                if (cp.tags != null) {
                    for (String tag : cp.tags) {
                        if (lower.contains(tag.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Scans and processes item, inventory, ender chest, and custom showcases from a message Component.
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
        boolean hasShulkerMatch = plainLower.contains("[shulker]");

        PluginConfig config = plugin.getConfigHandler().getConfig();
        PluginConfig.ItemShowcase settings = config.itemShowcase;
        PluginConfig.InventoryShowcase invSettings = config.inventoryShowcase;
        PluginConfig.EnderChestShowcase enderSettings = config.enderChestShowcase;

        boolean hasItemPermission = !settings.permissionRequired || player.hasPermission(settings.permissionNode);
        boolean hasInvPermission = !invSettings.permissionRequired || player.hasPermission(invSettings.permissionNode);
        boolean hasEnderPermission = !enderSettings.permissionRequired || player.hasPermission(enderSettings.permissionNode);
        boolean hasShulkerPermission = !settings.shulkerPermissionRequired || player.hasPermission(settings.shulkerPermissionNode);

        // Check for custom placeholder matches and permissions
        boolean hasCustomMatch = false;
        List<PluginConfig.CustomPlaceholder> matchedCustoms = new ArrayList<>();
        if (config.customPlaceholders != null) {
            for (PluginConfig.CustomPlaceholder cp : config.customPlaceholders.values()) {
                boolean hasPermission = cp.permissionNode == null || cp.permissionNode.isEmpty() || player.hasPermission(cp.permissionNode);
                if (!hasPermission) continue;

                boolean matched = false;
                for (String tag : cp.tags) {
                    if (plainLower.contains(tag.toLowerCase())) {
                        matched = true;
                        break;
                    }
                }
                if (matched) {
                    hasCustomMatch = true;
                    matchedCustoms.add(cp);
                }
            }
        }

        if (!hasItemMatch && !hasInvMatch && !hasEnderMatch && !hasShulkerMatch && !hasCustomMatch) {
            return new ProcessedResult(false, message, new ArrayList<>());
        }

        // If matched but no permission for any, fail fast
        if ((hasItemMatch && !hasItemPermission) && 
            (hasInvMatch && !hasInvPermission) && 
            (hasEnderMatch && !hasEnderPermission) &&
            (hasShulkerMatch && !hasShulkerPermission) &&
            matchedCustoms.isEmpty()) {
            return new ProcessedResult(false, message, new ArrayList<>());
        }

        // Fetch item in hand, inventory, and ender chest snapshot safely on the Main Tick Thread
        class ShowcaseDetails {
            final ItemStack itemStack;
            final HoverEvent<HoverEvent.ShowItem> itemHover;
            final InventorySnapshot invSnap;
            final EnderChestSnapshot enderSnap;
            final ShulkerSnapshot shulkerSnap;

            ShowcaseDetails(ItemStack itemStack, HoverEvent<HoverEvent.ShowItem> itemHover, 
                            InventorySnapshot invSnap, EnderChestSnapshot enderSnap, ShulkerSnapshot shulkerSnap) {
                this.itemStack = itemStack;
                this.itemHover = itemHover;
                this.invSnap = invSnap;
                this.enderSnap = enderSnap;
                this.shulkerSnap = shulkerSnap;
            }
        }

        CompletableFuture<ShowcaseDetails> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack inHand = null;
            HoverEvent<HoverEvent.ShowItem> inHandHover = null;
            ShulkerSnapshot sSnap = null;

            ItemStack item = player.getInventory().getItemInMainHand();
            boolean isHoldingShulker = item != null && !item.getType().isAir() && item.getType().name().endsWith("SHULKER_BOX");

            if ((hasItemMatch && hasItemPermission) || (hasShulkerMatch && hasShulkerPermission)) {
                if (item != null && !item.getType().isAir()) {
                    inHand = item.clone();
                    inHandHover = inHand.asHoverEvent();
                    
                    if (isHoldingShulker && ((hasItemMatch && hasItemPermission && (!settings.shulkerPermissionRequired || player.hasPermission(settings.shulkerPermissionNode))) || (hasShulkerMatch && hasShulkerPermission))) {
                        sSnap = plugin.getInventoryManager().createShulkerSnapshot(player, inHand);
                    }
                }
            }

            InventorySnapshot snap = null;
            if (hasInvMatch && hasInvPermission) {
                snap = plugin.getInventoryManager().createSnapshot(player);
            }

            EnderChestSnapshot eSnap = null;
            if (hasEnderMatch && hasEnderPermission) {
                eSnap = plugin.getInventoryManager().createEnderChestSnapshot(player);
            }

            future.complete(new ShowcaseDetails(inHand, inHandHover, snap, eSnap, sSnap));
        });

        ShowcaseDetails details;
        try {
            details = future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getComponentLogger().warn("Failed to fetch showcase details for player " + player.getName(), e);
            return new ProcessedResult(false, message, new ArrayList<>());
        }

        // If player typed [shulker] specifically but is not holding a shulker box, return error state privately
        if (hasShulkerMatch && hasShulkerPermission && details.shulkerSnap == null) {
            return new ProcessedResult(false, message, new ArrayList<>(), true,
                MiniMessage.miniMessage().deserialize(config.messages.noShulkerHeld));
        }

        List<ReplacementInstruction> replacements = new ArrayList<>();
        boolean replacedAny = false;

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
                Material material = itemStack.getType();
                if (material == Material.ENCHANTED_GOLDEN_APPLE) {
                    material = Material.GOLDEN_APPLE;
                }
                Component iconComponent = plugin.getCustomIconsHandler().getOverride(material, player);
                if (iconComponent == null && material == Material.PLAYER_HEAD) {
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

        Component shulkerReplacement = null;
        if (details.shulkerSnap != null) {
            String token = plugin.getInventoryManager().registerShulkerSnapshot(details.shulkerSnap);
            if (itemReplacement != null) {
                itemReplacement = itemReplacement.clickEvent(ClickEvent.runCommand("/chatitem viewshulker " + token));
            }
            shulkerReplacement = itemReplacement;
        }

        // Add built-in replacements
        if (details.itemStack != null) {
            replacements.add(new ReplacementInstruction(
                Pattern.compile("(?i)\\[(item|i)\\]"),
                itemReplacement
            ));
            replacedAny = true;
        } else if (hasItemMatch && hasItemPermission) {
            Component emptyReplacement = MiniMessage.miniMessage().deserialize(settings.emptyHandFormat);
            replacements.add(new ReplacementInstruction(
                Pattern.compile("(?i)\\[(item|i)\\]"),
                emptyReplacement
            ));
            replacedAny = true;
        }

        if (invReplacement != null) {
            replacements.add(new ReplacementInstruction(
                Pattern.compile("(?i)\\[(inventory|inv)\\]"),
                invReplacement
            ));
            replacedAny = true;
        }

        if (enderReplacement != null) {
            replacements.add(new ReplacementInstruction(
                Pattern.compile("(?i)\\[(echest|ender|enderchest)\\]"),
                enderReplacement
            ));
            replacedAny = true;
        }

        if (shulkerReplacement != null) {
            replacements.add(new ReplacementInstruction(
                Pattern.compile("(?i)\\[shulker\\]"),
                shulkerReplacement
            ));
            replacedAny = true;
        }

        // Add custom placeholder API replacements
        for (PluginConfig.CustomPlaceholder cp : matchedCustoms) {
            String parsedText = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, cp.displayFormat)
                : cp.displayFormat;
            Component rep = MiniMessage.miniMessage().deserialize(parsedText);

            if (cp.hoverFormat != null && !cp.hoverFormat.isEmpty()) {
                String parsedHover = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                    ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, cp.hoverFormat)
                    : cp.hoverFormat;
                rep = rep.hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize(parsedHover)));
            }

            if (cp.clickCommand != null && !cp.clickCommand.isEmpty()) {
                String parsedClick = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                    ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, cp.clickCommand)
                    : cp.clickCommand;
                rep = rep.clickEvent(ClickEvent.runCommand(parsedClick));
            }

            for (String tag : cp.tags) {
                replacements.add(new ReplacementInstruction(
                    Pattern.compile("(?i)" + Pattern.quote(tag)),
                    rep
                ));
            }
            replacedAny = true;
        }

        // Apply replacements to the final message Component
        Component finalMessage = message;
        for (ReplacementInstruction ri : replacements) {
            finalMessage = finalMessage.replaceText(TextReplacementConfig.builder()
                .match(ri.pattern)
                .replacement(ri.replacement)
                .build());
        }

        return new ProcessedResult(replacedAny, finalMessage, replacements);
    }
}
