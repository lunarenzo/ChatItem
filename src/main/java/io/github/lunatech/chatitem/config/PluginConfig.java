package io.github.lunatech.chatitem.config;

import io.github.lunatech.chatitem.config.exception.ConfigValidationException;
import io.github.lunatech.chatitem.config.migration.Migration;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Map;

@ConfigSerializable
public class PluginConfig implements VersionedConfig {
    @Comment("Do not change this value!")
    public int configVersion = 1;

    @Override
    @Exclude
    public int configVersion() {
        return configVersion;
    }

    @Override
    @Exclude
    public @NotNull Map<Integer, Migration> migrations() {
        return Map.of();
    }

    @Override
    @Exclude
    public void validate() throws ConfigValidationException {
    }

    @Comment("Update Checker Settings")
    public UpdateChecker updateChecker = new UpdateChecker();

    @ConfigSerializable
    public static class UpdateChecker {
        @Comment("Should the plugin check for plugin updates on startup?")
        public boolean enabled = true;

        @Comment("Send update notifications to the console?")
        public boolean console = true;

        @Comment("Send update notifications to opped players on join?")
        public boolean op = true;
    }

    @Comment("Language, specify the language file to use, for chatitem `en_US` which will load `/lang/en_US.json`")
    public String language = "en_US";

    @Comment("Item Showcase Settings")
    public ItemShowcase itemShowcase = new ItemShowcase();

    @ConfigSerializable
    public static class ItemShowcase {
        @Comment("Cooldown in seconds between item showcases.")
        public int cooldownSeconds = 3;

        @Comment("Is a permission required to use [item] or [i]?")
        public boolean permissionRequired = true;

        @Comment("The permission node required if permissionRequired is true.")
        public String permissionNode = "chatitem.showcase.item";

        @Comment("The MiniMessage format for the item tag in chat. Use <name> for the item name.")
        public String itemFormat = "<gray>[<aqua><name></aqua>]";

        @Comment("The MiniMessage format when player is holding nothing in main hand.")
        public String emptyHandFormat = "<gray><i>[Empty Hand]</i>";

        @Comment("Message sent to the player when they are on cooldown.")
        public String cooldownMessage = "<red>Please wait <cooldown>s before showcasing items again.";
    }

    @Comment("Custom Messages (Adventure MiniMessage color tags supported)")
    public Messages messages = new Messages();

    @ConfigSerializable
    public static class Messages {
        @Comment("Message sent when an inventory snapshot has expired or doesn't exist.")
        public String expired = "<red>This inventory snapshot has expired or is invalid.";

        @Comment("Message sent when a player specifies an invalid snapshot format.")
        public String invalidToken = "<red>Invalid Snapshot Token structure Specified.";

        @Comment("Hover text shown when hovering over the [inv] chat tag.")
        public String invHover = "<gray>Click to view double-chest inventory window.";

        @Comment("Title of the showcase inventory GUI.")
        public String invTitle = "<dark_gray>{player}'s Inventory Snapshot";
    }
}
